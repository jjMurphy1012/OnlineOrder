package com.laioffer.onlineorder;

import org.springframework.test.context.DynamicPropertyRegistry;

/** Explicit opt-in loopback database; never falls back to the application's default DB. */
final class PostgresTestDatabase {
    static void configure(DynamicPropertyRegistry registry) {
        String url = System.getenv("ONLINEORDER_TEST_JDBC_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/onlineorder_benchmark")) {
            throw new IllegalStateException("Use benchmarks/run_postgres_benchmark.py for an isolated DB");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "benchmark");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("onlineorder.demo-data.enabled", () -> "false");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("logging.level.org.springframework.jdbc", () -> "WARN");
    }
}
