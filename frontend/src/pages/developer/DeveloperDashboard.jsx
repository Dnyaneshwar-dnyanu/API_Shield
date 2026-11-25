import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { subscriptionApi } from '../../api/subscriptionApi';
import { apiKeyApi } from '../../api/apiKeyApi';
import { analyticsApi } from '../../api/analyticsApi';
import { 
  CreditCard, 
  Key, 
  Terminal, 
  Activity, 
  ArrowRight, 
  Zap, 
  Clock, 
  CheckCircle,
  AlertTriangle
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';

export const DeveloperDashboard = () => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [keys, setKeys] = useState([]);
  const [quota, setQuota] = useState(null);
  const [recentLogs, setRecentLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      subscriptionApi.getMySubscriptions({ page: 0, size: 5 }),
      apiKeyApi.getMyKeys({ page: 0, size: 5 }),
      analyticsApi.getDeveloperQuota(),
      analyticsApi.getDeveloperLogs({ page: 0, size: 5 }),
    ]).then(([subRes, keyRes, quotaRes, logRes]) => {
      if (subRes.status === 'fulfilled') setSubscriptions(subRes.value.content || []);
      if (keyRes.status === 'fulfilled') {
        const val = keyRes.value;
        setKeys(Array.isArray(val) ? val : (val?.content || []));
      }
      if (quotaRes.status === 'fulfilled') setQuota(quotaRes.value);
      if (logRes.status === 'fulfilled') setRecentLogs(logRes.value.content || []);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 flex justify-center">
        <div className="w-10 h-10 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Developer Dashboard</h1>
          <p className="text-slate-400 text-sm mt-1">
            Manage your active API subscriptions, cryptographic keys, and edge gateway consumption.
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Link
            to="/dev/playground"
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-750 text-slate-200 border border-slate-700 text-xs font-semibold flex items-center space-x-1.5 transition"
          >
            <Terminal className="w-4 h-4 text-emerald-400" />
            <span>Interactive Playground</span>
          </Link>
          <Link
            to="/marketplace"
            className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md flex items-center space-x-1.5 transition"
          >
            <Zap className="w-4 h-4" />
            <span>Explore APIs</span>
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Active Subscriptions */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Active Subscriptions
            </span>
            <div className="text-3xl font-extrabold text-white mt-1">
              {subscriptions.length}
            </div>
            <Link to="/dev/subscriptions" className="text-xs text-indigo-400 hover:underline mt-2 inline-block">
              Manage subscriptions →
            </Link>
          </div>
          <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
            <CreditCard className="w-6 h-6" />
          </div>
        </div>

        {/* Cryptographic Keys */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              API Keys Configured
            </span>
            <div className="text-3xl font-extrabold text-white mt-1">
              {keys.length}
            </div>
            <Link to="/dev/keys" className="text-xs text-emerald-400 hover:underline mt-2 inline-block">
              View & rotate keys →
            </Link>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Key className="w-6 h-6" />
          </div>
        </div>

        {/* Monthly Usage */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Current Month Requests
            </span>
            <div className="text-3xl font-extrabold text-white mt-1">
              {Number(quota?.currentMonthUsage || 0).toLocaleString()}
            </div>
            <span className="text-xs text-slate-400 mt-2 inline-block">
              Billing Period: {quota?.period || 'Current Month'}
            </span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400">
            <Activity className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Grid: Active Subscriptions & API Keys */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Subscriptions Card */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold text-white flex items-center space-x-2">
              <CreditCard className="w-4 h-4 text-indigo-400" />
              <span>My Active Subscriptions</span>
            </h3>
            <Link to="/dev/subscriptions" className="text-xs text-indigo-400 hover:underline">
              View All
            </Link>
          </div>

          {subscriptions.length === 0 ? (
            <div className="py-8 text-center text-slate-400 text-xs">
              No active subscriptions yet.
              <Link to="/marketplace" className="text-indigo-400 block mt-1 hover:underline">
                Browse Marketplace →
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {subscriptions.map((sub) => (
                <div
                  key={sub.id}
                  className="p-3.5 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between"
                >
                  <div>
                    <h4 className="text-sm font-semibold text-white">{sub.apiName || sub.apiId}</h4>
                    <p className="text-xs text-slate-400 font-mono mt-0.5">Plan: {sub.planName || sub.planId}</p>
                  </div>
                  <div className="text-right">
                    <StatusBadge status={sub.status} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* API Keys Card */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold text-white flex items-center space-x-2">
              <Key className="w-4 h-4 text-emerald-400" />
              <span>Active API Keys</span>
            </h3>
            <Link to="/dev/keys" className="text-xs text-emerald-400 hover:underline">
              Manage Keys
            </Link>
          </div>

          {keys.length === 0 ? (
            <div className="py-8 text-center text-slate-400 text-xs">
              No API keys generated.
              <Link to="/dev/keys" className="text-emerald-400 block mt-1 hover:underline">
                Generate an API key →
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {keys.map((k) => (
                <div
                  key={k.id}
                  className="p-3.5 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between"
                >
                  <div>
                    <h4 className="text-sm font-semibold text-white">{k.name}</h4>
                    <span className="text-xs font-mono text-indigo-300">
                      ash_live_{k.keyPrefix}_••••••••
                    </span>
                  </div>
                  <StatusBadge status={k.status} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent Request Logs */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white flex items-center space-x-2">
            <Clock className="w-4 h-4 text-sky-400" />
            <span>Recent Gateway Telemetry Logs</span>
          </h3>
          <Link to="/dev/usage" className="text-xs text-sky-400 hover:underline">
            Full Request History
          </Link>
        </div>

        {recentLogs.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-xs">
            No gateway calls logged yet. Test an endpoint in the{' '}
            <Link to="/dev/playground" className="text-indigo-400 underline">
              Interactive Playground
            </Link>.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-mono">
                  <th className="pb-2.5 font-normal">Method</th>
                  <th className="pb-2.5 font-normal">Endpoint Path</th>
                  <th className="pb-2.5 font-normal">Status</th>
                  <th className="pb-2.5 font-normal">Latency</th>
                  <th className="pb-2.5 font-normal">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {recentLogs.map((log) => (
                  <tr key={log.requestId} className="hover:bg-slate-850/50">
                    <td className="py-2.5 font-bold text-white">{log.httpMethod}</td>
                    <td className="py-2.5 text-slate-300">{log.endpointPath}</td>
                    <td className="py-2.5">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        log.statusCode >= 200 && log.statusCode < 300
                          ? 'bg-emerald-500/10 text-emerald-400'
                          : log.statusCode === 429
                          ? 'bg-amber-500/10 text-amber-400'
                          : 'bg-rose-500/10 text-rose-400'
                      }`}>
                        {log.statusCode}
                      </span>
                    </td>
                    <td className="py-2.5 text-slate-400">{log.latencyMs}ms</td>
                    <td className="py-2.5 text-slate-500">
                      {new Date(log.timestamp).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
