#!/usr/bin/env python3
"""Run synthetic integration tests in a fresh loopback PostgreSQL cluster, then remove it."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import socket
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="Fresh result directory")
    parser.add_argument("--pg-bin", type=Path, default=Path("/opt/homebrew/opt/postgresql@15/bin"))
    parser.add_argument("--java-home", type=Path, default=Path("/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"))
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    metadata = {
        "complete": False, "passed": False,
        "scope": "Synthetic fixtures in a new loopback-only PostgreSQL cluster, destroyed afterward",
        "baseline_commit": "383494f",
        "current_head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip(),
        "working_tree_dirty": bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=repo, text=True).strip()),
        "platform": platform.system(), "architecture": platform.machine(),
        "postgres": subprocess.check_output([str(args.pg_bin / "pg_ctl"), "--version"], text=True).strip(),
        "no_production_database": True,
        "timing": "Single process, sequential tests, warm DB pages/pool, no load generator or artificial DB delay",
    }
    manifest = out / "run-manifest.json"

    def write_manifest():
        manifest.write_text(json.dumps(metadata, indent=2) + "\n")

    write_manifest()
    provenance = out / "provenance"
    provenance.mkdir()
    sources = list((repo / "src").rglob("*.java")) + [repo / "build.gradle", repo / "src/main/resources/application.yml"]
    sources += list((repo / "benchmarks").glob("*.py")) + list((repo / "benchmarks").glob("*.sql"))
    hashes = {}
    for source in sources:
        relative = source.relative_to(repo)
        destination = provenance / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
        hashes[str(relative)] = hashlib.sha256(source.read_bytes()).hexdigest()
    metadata["source_sha256"] = hashes
    baseline = subprocess.check_output(["git", "show", "383494f:src/main/java/com/laioffer/onlineorder/service/CartService.java"], cwd=repo)
    (provenance / "CartService-383494f.java.txt").write_bytes(baseline)
    metadata["baseline_full_cart_service_sha256"] = hashlib.sha256(baseline).hexdigest()
    (provenance / "tracked-change.patch").write_bytes(subprocess.check_output(["git", "diff", "--binary"], cwd=repo))
    write_manifest()

    with tempfile.TemporaryDirectory(prefix="onlineorder-pg-") as temp:
        temp_path = Path(temp)
        data = temp_path / "data"
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            port = sock.getsockname()[1]
        subprocess.run([str(args.pg_bin / "initdb"), "-D", str(data), "-U", "benchmark", "--auth=trust", "--no-locale", "-E", "UTF8"], check=True, capture_output=True)
        started = False
        try:
            subprocess.run([str(args.pg_bin / "pg_ctl"), "-D", str(data), "-l", str(out / "postgres.log"), "-o", f"-h 127.0.0.1 -p {port} -k {temp_path}", "-w", "start"], check=True, capture_output=True)
            started = True
            subprocess.run([str(args.pg_bin / "createdb"), "-h", "127.0.0.1", "-p", str(port), "-U", "benchmark", "onlineorder_benchmark"], check=True, capture_output=True)
            env = os.environ.copy()
            env.update({
                "JAVA_HOME": str(args.java_home),
                "ONLINEORDER_TEST_JDBC_URL": f"jdbc:postgresql://127.0.0.1:{port}/onlineorder_benchmark",
                "ONLINEORDER_BENCH_OUTPUT": str(out),
            })
            start = time.perf_counter()
            with (out / "gradle.log").open("w") as log:
                process = subprocess.run([str(repo / "gradlew"), "--no-daemon", "test", "--rerun-tasks"], cwd=repo, env=env, stdout=log, stderr=subprocess.STDOUT)
            metadata["test_command_exit"] = process.returncode
            metadata["suite_wall_seconds"] = time.perf_counter() - start
            xml_dir = out / "junit"
            xml_dir.mkdir()
            counts = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
            for xml in (repo / "build/test-results/test").glob("TEST-*.xml"):
                shutil.copy2(xml, xml_dir / xml.name)
                root = ET.parse(xml).getroot()
                for name in counts:
                    counts[name] += int(root.attrib.get(name, "0"))
            metadata["junit_counts"] = counts
            metadata["complete"] = True
            metadata["passed"] = process.returncode == 0 and counts["tests"] > 0 and counts["failures"] == counts["errors"] == counts["skipped"] == 0
            for path in [out / "gradle.log", *xml_dir.glob("*.xml")]:
                # Preserve test/SQL evidence while removing machine-specific home paths.
                content = path.read_text().replace(str(repo), "<repository>").replace(str(Path.home()), "<home>")
                path.write_text(content)
        finally:
            if started:
                subprocess.run([str(args.pg_bin / "pg_ctl"), "-D", str(data), "-m", "immediate", "-w", "stop"], check=True, capture_output=True)
            metadata["isolated_cluster_stopped"] = True
            write_manifest()
    print(json.dumps({key: metadata.get(key) for key in ["complete", "passed", "junit_counts", "suite_wall_seconds", "isolated_cluster_stopped"]}, indent=2))
    return 0 if metadata["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
