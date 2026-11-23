import React, { useState, useEffect } from 'react';
import { ownerApi } from '../../api/ownerApi';
import { analyticsApi } from '../../api/analyticsApi';
import { BarChart3, TrendingUp, Clock, AlertCircle } from 'lucide-react';

export const OwnerAnalyticsPage = () => {
  const [apis, setApis] = useState([]);
  const [selectedApiId, setSelectedApiId] = useState('');
  const [days, setDays] = useState(7);
  const [overview, setOverview] = useState(null);
  const [timeseries, setTimeseries] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ownerApi.getMyApis({ page: 0, size: 50 })
      .then((data) => {
        const list = data.content || [];
        setApis(list);
        if (list.length > 0) setSelectedApiId(list[0].id);
      })
      .catch(() => setApis([]));
  }, []);

  useEffect(() => {
    if (!selectedApiId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    Promise.allSettled([
      analyticsApi.getOwnerOverview({ apiId: selectedApiId, days }),
      analyticsApi.getApiTimeseries(selectedApiId, days),
    ]).then(([overviewRes, timeRes]) => {
      if (overviewRes.status === 'fulfilled') setOverview(overviewRes.value);
      if (timeRes.status === 'fulfilled') setTimeseries(timeRes.value || []);
      setLoading(false);
    });
  }, [selectedApiId, days]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Header & Filter Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight flex items-center space-x-2">
            <BarChart3 className="w-7 h-7 text-indigo-400" />
            <span>Traffic & Telemetry Analytics</span>
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Timeseries call volumes, error distributions, and p95 latency percentiles.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <select
            value={selectedApiId}
            onChange={(e) => setSelectedApiId(e.target.value)}
            className="px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-slate-200 focus:outline-none"
          >
            {apis.map((a) => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>

          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            className="px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-slate-200 focus:outline-none"
          >
            <option value={7}>Last 7 Days</option>
            <option value={14}>Last 14 Days</option>
            <option value={30}>Last 30 Days</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="py-20 flex justify-center">
          <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
        </div>
      ) : (
        <>
          {/* KPI Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Total Requests
              </span>
              <div className="text-3xl font-extrabold text-white mt-1">
                {Number(overview?.totalRequests || 0).toLocaleString()}
              </div>
              <span className="text-xs text-emerald-400 mt-2 block">
                {Number(overview?.successfulRequests || 0).toLocaleString()} Successful
              </span>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Average Latency
              </span>
              <div className="text-3xl font-extrabold text-white mt-1">
                {overview?.averageLatencyMs || 0}ms
              </div>
              <span className="text-xs text-slate-400 mt-2 block">
                Rolling Mean execution
              </span>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                p95 Latency
              </span>
              <div className="text-3xl font-extrabold text-white mt-1">
                {overview?.p95LatencyMs || 0}ms
              </div>
              <span className="text-xs text-indigo-400 mt-2 block">
                95% of calls served under this limit
              </span>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Error Rate
              </span>
              <div className="text-3xl font-extrabold text-white mt-1">
                {overview?.errorRate || 0}%
              </div>
              <span className="text-xs text-rose-400 mt-2 block">
                {overview?.errorCount || 0} Non-2xx Responses
              </span>
            </div>
          </div>

          {/* Timeseries Breakdown Table */}
          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
            <h2 className="text-base font-bold text-white flex items-center space-x-2">
              <TrendingUp className="w-4 h-4 text-emerald-400" />
              <span>Timeseries Aggregation Rollups</span>
            </h2>

            {timeseries.length === 0 ? (
              <div className="text-center py-12 text-slate-400 text-xs">
                No telemetry rollups recorded in this window. Send traffic via the API Gateway to generate metrics.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs font-mono">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400">
                      <th className="pb-3 font-normal">Timestamp</th>
                      <th className="pb-3 font-normal">Total Requests</th>
                      <th className="pb-3 font-normal">Success (2xx)</th>
                      <th className="pb-3 font-normal">Client Errors (4xx)</th>
                      <th className="pb-3 font-normal">Server Errors (5xx)</th>
                      <th className="pb-3 font-normal text-right">Avg Latency</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {timeseries.map((pt, idx) => (
                      <tr key={idx} className="hover:bg-slate-850/50">
                        <td className="py-3 text-slate-300 font-semibold">{pt.timestamp}</td>
                        <td className="py-3 text-white font-bold">{pt.totalRequests}</td>
                        <td className="py-3 text-emerald-400">{pt.successfulRequests}</td>
                        <td className="py-3 text-amber-400">{pt.clientErrors}</td>
                        <td className="py-3 text-rose-400">{pt.serverErrors}</td>
                        <td className="py-3 text-right text-indigo-300">{pt.avgLatencyMs}ms</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
