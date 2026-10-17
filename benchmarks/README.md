# Reproducible PostgreSQL verification

## Run

Requires Java 21, Python 3, and local PostgreSQL binaries. Run from the repository root:

```sh
python3 benchmarks/run_postgres_benchmark.py --output /tmp/onlineorder-results-new
```

The result directory must not already exist. Override `--pg-bin` and `--java-home` for your installation. The launcher creates a fresh cluster, binds only to loopback on a dynamically selected port, creates `onlineorder_benchmark`, runs all tests, and stops/removes the cluster. It never uses the application's default database. Default `./gradlew test` skips database-dependent tests without this explicit environment; use the launcher for the full gate.

## Recorded result (2026-10-16)

`results/20261016/` contains the accepted run's raw samples, summary, manifest/source hashes and JUnit reports. The full provenance snapshot is retained in the resume workspace evidence directory. The earlier preliminary run predates instrumentation/rollback-oracle refinements and is not used for resume claims.

- Java 21.0.4; Spring Boot 3.4.4; Gradle 8.13; PostgreSQL 15.10; macOS arm64.
- 100 synthetic restaurants, 1,000 menu items; carts with 1/10/50/100 distinct line items.
- 16 tests passed, zero skipped: 4 original cart unit tests, 4 new boundary regressions, 7 PostgreSQL/HTTP integration tests, 1 application-context test.
- Each variant: 10 warmup + 50 measured samples; old/new order alternates AB/BA. SQL evidence and timing-only passes are separate. There are 1,200 recorded observations (1,000 measured, 200 warmups).

| Cart lines | SELECTs old/new | p50 old/new (ms) | p95 old/new (ms) |
|---:|---:|---:|---:|
| 1 | 3 / 3 | 0.316 / 0.333 | 0.511 / 0.462 |
| 10 | 12 / 3 | 1.280 / 0.600 | 1.750 / 0.864 |
| 50 | 52 / 3 | 5.445 / 0.717 | 8.047 / 1.421 |
| 100 | 102 / 3 | 10.329 / 1.142 | 17.281 / 1.746 |

At 100 lines, p50 decreased 88.95%, p95 89.89%, and SELECT count 97.06%. These are local fixed-fixture service measurements. At one line, batching does not reduce queries and the observed p50 is slightly higher; it is not universally faster.

## Measurement design

`LegacyCartRead` preserves the exact read algorithm from commit `383494f`. Both arms use the same real Spring Data JDBC repository beans and bypass cart cache advice. The current service batches distinct menu IDs and maps results back to original order rows. Every observation is checked against a fixture-derived oracle for all DTO fields and totals, and against the other arm. Setup, cache clearing and assertions are outside timers.

The reported timings come from `_timing_only` groups with SQL collection disabled. The common JDBC proxy remains installed. A missing `sql_count` field (or null in the grouped summary) means unobserved, not zero. Separate traced groups verify actual JDBC executions, record query templates, and prove N+2 versus 3 SELECTs. SQL strings are normalized only after timers stop. Passes run sequentially, so their difference is not a controlled estimate of profiler overhead; JVM warming also differs. Percentiles use nearest rank `ceil(p*n)`, including the lower middle observation for p50 at n=50.

Timings include service work, database round trips, driver/repository mapping and common instrumentation scaffolding. They exclude HTTP, authentication/customer lookup, JSON serialization, browser/network delay and application startup. PostgreSQL pages and connection pools are warm. This is not a load or concurrency test and no synthetic DB delay is inserted.

## Cache and correctness checks

The actual runtime uses Caffeine, configured for 60-second expire-after-write and a maximum of 1,000 entries per cache. Catalog miss/hit comparison uses the same current service and 100/1,000 fixture: SELECTs 2/0, p50 4.077/0.037 ms, p95 5.725/0.062 ms. This compares cache states, not a historical implementation speedup. The expiry test verifies the effective 60-second policy then temporarily shortens it to 15 ms to test expiry/reload, restoring it afterward; it does not wait 60 seconds.

Real PostgreSQL tests verify affected-customer cache invalidation, isolation of another customer's cache, and add/clear rollback. Triggers deliberately reject the cart-total UPDATE after an item INSERT or DELETE; assertions require PostgreSQL SQLState P0001, attempted mutation ordering, restored rows/totals and unchanged cached data. Real HTTP tests cover registration/login, anonymous denial, principal-derived cart isolation, add/clear and logout. These tests are not proof of concurrent-write safety or a comprehensive security audit.

## Interpretation boundaries

The HTTP cart route also resolves a customer by email, so 102-to-3 is a service-layer count. No production user count, cloud throughput, API SLA, payment flow, or cost reduction is inferred. Checkout only clears a cart. Cross-instance cache invalidation, CSRF, concurrent lost updates, monetary precision and production-safe migrations remain separate work. Raw fixtures contain only invented data.
