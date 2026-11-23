/**
 * APIShield - Mock Upstream Target Server
 * Simulates a third-party backend API service registered by an API Owner.
 * Used for end-to-end data plane proxy verification and latency benchmarking.
 */

const http = require('http');

const PORT = parseInt(process.env.MOCK_PORT || '9999', 10);

const server = http.createServer((req, res) => {
  const startTime = Date.now();
  let body = '';

  req.on('data', chunk => {
    body += chunk;
  });

  req.on('end', () => {
    // Return mock weather data or echo response
    const responsePayload = {
      service: 'mock-weather-service',
      version: '1.0.0',
      timestamp: new Date().toISOString(),
      requestedPath: req.url,
      method: req.method,
      clientHeaders: {
        'x-correlation-id': req.headers['x-correlation-id'],
        'x-consumer-id': req.headers['x-consumer-id'],
        'x-api-key-id': req.headers['x-api-key-id'],
        'x-apishield-proxy': req.headers['x-apishield-proxy']
      },
      data: {
        city: 'San Francisco',
        temperature: 18.5,
        unit: 'celsius',
        condition: 'Clear Skies',
        humidity: 62,
        windSpeedKmH: 14.2
      },
      upstreamProcessingTimeMs: Date.now() - startTime
    };

    res.writeHead(200, {
      'Content-Type': 'application/json',
      'X-Mock-Upstream': 'APIShield-Target-V1'
    });
    res.end(JSON.stringify(responsePayload, null, 2));
  });
});

if (require.main === module) {
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`[Mock Upstream] Server running on http://localhost:${PORT}`);
  });
}

module.exports = server;
