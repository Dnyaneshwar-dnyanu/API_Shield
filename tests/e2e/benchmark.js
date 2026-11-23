/**
 * APIShield - Performance Benchmarking Engine
 * Measures Edge Gateway throughput, concurrency handling, and latency percentiles (P50, P90, P95, P99).
 */

const { performance } = require('perf_hooks');

class BenchmarkEngine {
  constructor(options = {}) {
    this.concurrency = options.concurrency || 10;
    this.totalRequests = options.totalRequests || 100;
    this.url = options.url || 'http://localhost:8080/gateway/weather-api/current';
    this.headers = options.headers || {};
    this.method = options.method || 'GET';
    this.body = options.body || null;
  }

  calculatePercentile(sortedLatencies, percentile) {
    if (sortedLatencies.length === 0) return 0;
    const index = Math.ceil((percentile / 100) * sortedLatencies.length) - 1;
    return sortedLatencies[Math.max(0, Math.min(index, sortedLatencies.length - 1))];
  }

  async runRequest() {
    const start = performance.now();
    try {
      const response = await fetch(this.url, {
        method: this.method,
        headers: this.headers,
        body: this.body
      });
      const duration = performance.now() - start;
      return {
        status: response.status,
        duration,
        success: response.ok
      };
    } catch (err) {
      const duration = performance.now() - start;
      return {
        status: 0,
        duration,
        success: false,
        error: err.message
      };
    }
  }

  async run() {
    console.log(`\n============================================================`);
    console.log(`🚀 APIShield Gateway Benchmark Starting`);
    console.log(`   Target URL:     ${this.url}`);
    console.log(`   Total Requests: ${this.totalRequests}`);
    console.log(`   Concurrency:    ${this.concurrency}`);
    console.log(`============================================================\n`);

    const results = [];
    const overallStart = performance.now();

    // Run in concurrency pools
    let executed = 0;
    const pool = [];

    const executeNext = async () => {
      while (executed < this.totalRequests) {
        executed++;
        const res = await this.runRequest();
        results.push(res);
      }
    };

    for (let i = 0; i < this.concurrency; i++) {
      pool.push(executeNext());
    }

    await Promise.all(pool);
    const overallElapsed = performance.now() - overallStart;

    // Compute Metrics
    const durations = results.map(r => r.duration).sort((a, b) => a - b);
    const totalCount = results.length;
    const successCount = results.filter(r => r.success).length;
    const rateLimitedCount = results.filter(r => r.status === 429).length;
    const errorCount = results.filter(r => !r.success && r.status !== 429).length;

    const minLatency = durations[0] || 0;
    const maxLatency = durations[durations.length - 1] || 0;
    const sumLatency = durations.reduce((acc, val) => acc + val, 0);
    const meanLatency = totalCount > 0 ? sumLatency / totalCount : 0;

    const p50 = this.calculatePercentile(durations, 50);
    const p75 = this.calculatePercentile(durations, 75);
    const p90 = this.calculatePercentile(durations, 90);
    const p95 = this.calculatePercentile(durations, 95);
    const p99 = this.calculatePercentile(durations, 99);

    const rps = (totalCount / (overallElapsed / 1000)).toFixed(2);

    const summary = {
      totalRequests: totalCount,
      successCount,
      rateLimitedCount,
      errorCount,
      overallElapsedMs: overallElapsed.toFixed(2),
      throughputRps: parseFloat(rps),
      latency: {
        minMs: minLatency.toFixed(2),
        maxMs: maxLatency.toFixed(2),
        meanMs: meanLatency.toFixed(2),
        p50Ms: p50.toFixed(2),
        p75Ms: p75.toFixed(2),
        p90Ms: p90.toFixed(2),
        p95Ms: p95.toFixed(2),
        p99Ms: p99.toFixed(2)
      }
    };

    this.printReport(summary);
    return summary;
  }

  printReport(summary) {
    console.log(`------------------------------------------------------------`);
    console.log(`📊 BENCHMARK RESULTS SUMMARY`);
    console.log(`------------------------------------------------------------`);
    console.log(`  Total Completed:    ${summary.totalRequests}`);
    console.log(`  Successful (2xx):   ${summary.successCount}`);
    console.log(`  Rate Limited (429): ${summary.rateLimitedCount}`);
    console.log(`  Other Errors:       ${summary.errorCount}`);
    console.log(`  Total Duration:     ${summary.overallElapsedMs} ms`);
    console.log(`  Throughput:         ${summary.throughputRps} requests/sec`);
    console.log(`------------------------------------------------------------`);
    console.log(`⏱️  LATENCY PERCENTILES`);
    console.log(`------------------------------------------------------------`);
    console.log(`  Min:  ${summary.latency.minMs} ms`);
    console.log(`  Mean: ${summary.latency.meanMs} ms`);
    console.log(`  P50:  ${summary.latency.p50Ms} ms`);
    console.log(`  P75:  ${summary.latency.p75Ms} ms`);
    console.log(`  P90:  ${summary.latency.p90Ms} ms`);
    console.log(`  P95:  ${summary.latency.p95Ms} ms`);
    console.log(`  P99:  ${summary.latency.p99Ms} ms`);
    console.log(`  Max:  ${summary.latency.maxMs} ms`);
    console.log(`============================================================\n`);
  }
}

if (require.main === module) {
  const url = process.argv[2] || 'http://localhost:8080/gateway/weather-api/current';
  const apiKey = process.argv[3] || 'ash_live_test_key';
  const total = parseInt(process.argv[4] || '100', 10);
  const concurrency = parseInt(process.argv[5] || '10', 10);

  const engine = new BenchmarkEngine({
    url,
    headers: { 'X-API-Key': apiKey },
    totalRequests: total,
    concurrency
  });

  engine.run().catch(console.error);
}

module.exports = BenchmarkEngine;
