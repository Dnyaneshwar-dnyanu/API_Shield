import React from 'react';
import { Shield, GitBranch, Cpu, Server } from 'lucide-react';

export const Footer = () => {
  return (
    <footer className="bg-slate-950 border-t border-slate-800/80 text-slate-400 py-10 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          {/* Brand & Overview */}
          <div className="md:col-span-2 space-y-3">
            <div className="flex items-center space-x-2">
              <Shield className="w-5 h-5 text-indigo-400" />
              <span className="text-base font-bold text-white tracking-tight">APIShield</span>
            </div>
            <p className="text-sm text-slate-400 max-w-sm">
              Enterprise-grade API Management, Security, and Monetization platform. High-performance reactive Edge Gateway with sub-millisecond rate limiting and cryptographic authorization.
            </p>
            <div className="flex items-center space-x-4 text-xs font-mono text-slate-500 pt-2">
              <span className="flex items-center space-x-1">
                <Server className="w-3.5 h-3.5 text-indigo-400" />
                <span>9 Microservices</span>
              </span>
              <span>•</span>
              <span className="flex items-center space-x-1">
                <Cpu className="w-3.5 h-3.5 text-emerald-400" />
                <span>Spring Cloud Gateway</span>
              </span>
              <span>•</span>
              <span className="flex items-center space-x-1">
                <GitBranch className="w-3.5 h-3.5 text-sky-400" />
                <span>Kafka KRaft</span>
              </span>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200 mb-3">
              Platform
            </h4>
            <ul className="space-y-2 text-sm">
              <li><a href="/marketplace" className="hover:text-white transition">API Marketplace</a></li>
              <li><a href="/dev/playground" className="hover:text-white transition">API Playground</a></li>
              <li><a href="/owner/dashboard" className="hover:text-white transition">API Monetization</a></li>
              <li><a href="/marketplace" className="hover:text-white transition">Documentation</a></li>
            </ul>
          </div>

          {/* System Telemetry Info */}
          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200 mb-3">
              Infrastructure
            </h4>
            <ul className="space-y-1.5 text-xs font-mono text-slate-400">
              <li className="flex items-center justify-between">
                <span>Gateway:</span>
                <span className="text-emerald-400">Netty :8080</span>
              </li>
              <li className="flex items-center justify-between">
                <span>Discovery:</span>
                <span className="text-indigo-400">Eureka :8761</span>
              </li>
              <li className="flex items-center justify-between">
                <span>Rate Limiter:</span>
                <span className="text-rose-400">Redis Lua</span>
              </li>
              <li className="flex items-center justify-between">
                <span>Event Stream:</span>
                <span className="text-sky-400">Kafka :9092</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="pt-6 border-t border-slate-800/60 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500">
          <p>© {new Date().getFullYear()} APIShield Platform. All rights reserved.</p>
          <p className="mt-2 sm:mt-0 font-mono">Build v1.0.0-PROD • 237 Automated Reactor Tests Verified</p>
        </div>
      </div>
    </footer>
  );
};
