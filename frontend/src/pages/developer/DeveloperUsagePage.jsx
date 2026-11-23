import React, { useState, useEffect } from 'react';
import { analyticsApi } from '../../api/analyticsApi';
import { Activity, Clock, BarChart2 } from 'lucide-react';

export const DeveloperUsagePage = () => {
  const [quota, setQuota] = useState(null);
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      analyticsApi.getDeveloperQuota(),
      analyticsApi.getDeveloperLogs({ page, size: 20 }),
    ]).then(([quotaRes, logRes]) => {
      if (quotaRes.status === 'fulfilled') setQuota(quotaRes.value);
      if (logRes.status === 'fulfilled') {
        setLogs(logRes.value.content || []);
        setTotalPages(logRes.value.totalPages || 1);
      }
      setLoading(false);
    });
  }, [page]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      <div className="pb-6 border-b border-slate-800">
        <h1 className="text-3xl font-extrabold text-white tracking-tight">Usage & Telemetry</h1>
        <p className="text-slate-400 text-sm mt-1">
          Detailed request audit log and monthly quota consumption.
        </p>
      </div>

      {/* Quota Overview Card */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <span className="text-xs font-mono uppercase tracking-wider text-slate-400">
            Total Ingested Telemetry Calls
          </span>
          <div className="text-4xl font-extrabold text-white mt-1">
            {Number(quota?.currentMonthUsage || 0).toLocaleString()}
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Billing Period: <span className="font-mono text-indigo-300">{quota?.period || 'Current Month'}</span>
          </p>
        </div>
      </div>

      {/* Logs Table */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <Clock className="w-4 h-4 text-sky-400" />
            <span>Request Telemetry Logs</span>
          </h2>
          <span className="text-xs font-mono text-slate-500">
            Page {page + 1} of {totalPages}
          </span>
        </div>

        {loading ? (
          <div className="py-20 flex justify-center">
            <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
          </div>
        ) : logs.length === 0 ? (
          <div className="py-12 text-center text-slate-400 text-xs">
            No telemetry logs recorded yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400">
                  <th className="pb-3 font-normal">Method</th>
                  <th className="pb-3 font-normal">Path</th>
                  <th className="pb-3 font-normal">Status</th>
                  <th className="pb-3 font-normal">Latency</th>
                  <th className="pb-3 font-normal">Client IP</th>
                  <th className="pb-3 font-normal">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {logs.map((item) => (
                  <tr key={item.requestId} className="hover:bg-slate-850/50">
                    <td className="py-3 font-bold text-white">{item.httpMethod}</td>
                    <td className="py-3 text-slate-300">{item.endpointPath}</td>
                    <td className="py-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        item.statusCode >= 200 && item.statusCode < 300
                          ? 'bg-emerald-500/10 text-emerald-400'
                          : item.statusCode === 429
                          ? 'bg-amber-500/10 text-amber-400'
                          : 'bg-rose-500/10 text-rose-400'
                      }`}>
                        {item.statusCode}
                      </span>
                    </td>
                    <td className="py-3 text-slate-400">{item.latencyMs}ms</td>
                    <td className="py-3 text-slate-500">{item.clientIp || '127.0.0.1'}</td>
                    <td className="py-3 text-slate-500">
                      {new Date(item.timestamp).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t border-slate-800">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3.5 py-1.5 rounded-lg bg-slate-850 text-xs font-medium disabled:opacity-40"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3.5 py-1.5 rounded-lg bg-slate-850 text-xs font-medium disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
