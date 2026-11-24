
## Week 9 — Load Test Results

**Date:** November 24, 2026  
**CPU:** AMD Ryzen 7 7840HS w/ Radeon 780M Graphics  
**Worker threads:** 4  
**Test setup:** JobForge application, PostgreSQL database, and load-test script running on the same laptop.

| Run | Jobs | Submit time | Submit rate | End-to-end time | End-to-end throughput |
|---|---:|---:|---:|---:|---:|
| 1 | 200 | 2.73 s | 73.3 jobs/s | 3.58 s | 55.8 jobs/s |
| 2 | 500 | 5.93 s | 84.3 jobs/s | 6.66 s | 75.0 jobs/s |
| 3 | 500 | 5.90 s | 84.7 jobs/s | 6.30 s | 79.4 jobs/s |

**Verification:** Prometheus confirmed HTTP 201 for all 1,200 submitted jobs. The queue drained to zero queued and running jobs after each test.

**Note:** These are local laptop benchmark results, not production capacity figures. The submission rate measures how quickly the script submitted jobs; end-to-end throughput includes processing and queue-drain time.
