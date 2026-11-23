import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Shield, 
  Zap, 
  Lock, 
  BarChart3, 
  ArrowRight, 
  CheckCircle, 
  Server, 
  Database, 
  Activity,
  Layers,
  Terminal,
  Cpu
} from 'lucide-react';
import { catalogApi } from '../../api/catalogApi';

export const LandingPage = () => {
  const [featuredApis, setFeaturedApis] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    catalogApi.listCatalog({ page: 0, size: 3 })
      .then((data) => setFeaturedApis(data.content || []))
      .catch(() => setFeaturedApis([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="relative overflow-hidden">
      {/* Background Glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-7xl h-[500px] bg-gradient-to-b from-indigo-600/15 via-purple-600/5 to-transparent blur-3xl pointer-events-none" />

      {/* Hero Section */}
      <section className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24 text-center">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-semibold mb-8 animate-fade-in">
          <span className="flex h-2 w-2 rounded-full bg-indigo-400 animate-ping" />
          <span>Built on Spring Cloud Gateway & Java 21 LTS</span>
        </div>

        <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white max-w-5xl mx-auto leading-tight sm:leading-none">
          The Enterprise API Management &{' '}
          <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-sky-400 bg-clip-text text-transparent">
            Security Platform
          </span>
        </h1>

        <p className="mt-6 text-lg sm:text-xl text-slate-300 max-w-3xl mx-auto font-normal">
          Empowering API providers to publish, secure, and monetize backend endpoints, while providing developers with sub-millisecond edge proxying, cryptographic API keys, and real-time usage telemetry.
        </p>

        <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            to="/marketplace"
            className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 flex items-center justify-center space-x-2 transition duration-200"
          >
            <span>Explore API Marketplace</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
          <Link
            to="/register"
            className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 border border-slate-800 font-semibold transition"
          >
            Publish Your APIs
          </Link>
        </div>

        {/* Live Architecture Status Bar */}
        <div className="mt-16 grid grid-cols-2 md:grid-cols-4 gap-4 max-w-4xl mx-auto text-left">
          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur-sm">
            <div className="flex items-center space-x-2 text-indigo-400 text-xs font-mono mb-1">
              <Zap className="w-3.5 h-3.5" />
              <span>EDGE GATEWAY</span>
            </div>
            <div className="text-lg font-bold text-white">&lt; 1 ms</div>
            <p className="text-[11px] text-slate-400">Token Bucket Lua Rate Limiter</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur-sm">
            <div className="flex items-center space-x-2 text-emerald-400 text-xs font-mono mb-1">
              <Lock className="w-3.5 h-3.5" />
              <span>SECURITY</span>
            </div>
            <div className="text-lg font-bold text-white">SHA-256</div>
            <p className="text-[11px] text-slate-400">Cryptographic Key Hashes</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur-sm">
            <div className="flex items-center space-x-2 text-sky-400 text-xs font-mono mb-1">
              <Activity className="w-3.5 h-3.5" />
              <span>STREAMING</span>
            </div>
            <div className="text-lg font-bold text-white">Kafka KRaft</div>
            <p className="text-[11px] text-slate-400">Async Telemetry Ingestion</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur-sm">
            <div className="flex items-center space-x-2 text-purple-400 text-xs font-mono mb-1">
              <Server className="w-3.5 h-3.5" />
              <span>ARCHITECTURE</span>
            </div>
            <div className="text-lg font-bold text-white">9 Services</div>
            <p className="text-[11px] text-slate-400">Database-per-Service Isolation</p>
          </div>
        </div>
      </section>

      {/* Core Platform Tenets */}
      <section className="py-20 bg-slate-900/40 border-y border-slate-800/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto mb-16">
            <h2 className="text-xs font-bold uppercase tracking-wider text-indigo-400">
              Enterprise Resilience
            </h2>
            <p className="text-3xl font-extrabold text-white mt-2">
              Everything you need to secure, govern, and scale APIs
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/50 transition">
              <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-5">
                <Terminal className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">9-Phase Reactive Proxy</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Non-blocking high-throughput gateway executing correlation injection, route resolution, SSRF validation, Redis key auth, token bucket bursts, and monthly quotas.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-purple-500/50 transition">
              <div className="w-12 h-12 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 mb-5">
                <Lock className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Cryptographic Key Governance</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Generate <code className="text-xs bg-slate-800 px-1 py-0.5 rounded text-indigo-300">ash_live_...</code> keys returned once to the caller. Store only SHA-256 digests in MySQL and Redis with zero plaintext leakage.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-sky-500/50 transition">
              <div className="w-12 h-12 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400 mb-5">
                <BarChart3 className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Real-Time Telemetry & Billing</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Asynchronous telemetry events stream to Kafka for hourly rollups, computing p95 latencies, error distributions, Razorpay payments, and quota resets.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Featured APIs CTA */}
      <section className="py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-10">
          <div>
            <h2 className="text-xs font-bold uppercase tracking-wider text-indigo-400">Catalog Preview</h2>
            <p className="text-2xl sm:text-3xl font-extrabold text-white mt-1">
              Explore Active APIs on APIShield
            </p>
          </div>
          <Link
            to="/marketplace"
            className="text-sm font-semibold text-indigo-400 hover:text-indigo-300 flex items-center space-x-1 mt-4 md:mt-0"
          >
            <span>View All APIs</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {featuredApis.length > 0 ? (
            featuredApis.map((api) => (
              <div
                key={api.id}
                className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between hover:border-slate-700 transition"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-semibold px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      {api.category || 'API'}
                    </span>
                    <span className="text-xs text-slate-500 font-mono">v{api.version || '1.0'}</span>
                  </div>
                  <h3 className="text-lg font-bold text-white">{api.name}</h3>
                  <p className="text-xs font-mono text-indigo-300 mt-1">/gateway/{api.slug}/**</p>
                  <p className="text-sm text-slate-400 mt-3 line-clamp-2">
                    {api.description || 'Enterprise API ready for integration.'}
                  </p>
                </div>

                <div className="pt-6 mt-6 border-t border-slate-800 flex items-center justify-between">
                  <span className="text-xs text-slate-400">
                    {api.endpointsCount || 1} Endpoints
                  </span>
                  <Link
                    to={`/marketplace/${api.slug}`}
                    className="text-xs font-semibold text-indigo-400 hover:text-white flex items-center space-x-1"
                  >
                    <span>Inspect</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            ))
          ) : (
            <div className="col-span-3 text-center py-12 rounded-2xl bg-slate-900/50 border border-dashed border-slate-800">
              <Layers className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400 font-medium">Marketplace catalog is live</p>
              <Link
                to="/marketplace"
                className="inline-flex items-center space-x-1 text-sm font-semibold text-indigo-400 hover:underline mt-2"
              >
                <span>Browse Marketplace</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          )}
        </div>
      </section>
    </div>
  );
};
