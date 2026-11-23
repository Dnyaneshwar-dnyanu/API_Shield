import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ownerApi } from '../../api/ownerApi';
import { analyticsApi } from '../../api/analyticsApi';
import { 
  Layers, 
  BarChart2, 
  Plus, 
  Activity, 
  CheckCircle, 
  Clock, 
  ArrowRight,
  TrendingUp,
  AlertTriangle
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';

export const OwnerDashboard = () => {
  const [apis, setApis] = useState([]);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      ownerApi.getMyApis({ page: 0, size: 10 }),
      analyticsApi.getOwnerOverview({ days: 30 }),
    ]).then(([apisRes, overviewRes]) => {
      if (apisRes.status === 'fulfilled') setApis(apisRes.value.content || []);
      if (overviewRes.status === 'fulfilled') setOverview(overviewRes.value);
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
      {/* Top Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">API Owner Console</h1>
          <p className="text-slate-400 text-sm mt-1">
            Monetize backend services, manage endpoints, and track real-time edge telemetry.
          </p>
        </div>
        <Link
          to="/owner/apis/new"
          className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs shadow-md shadow-indigo-600/30 flex items-center space-x-2 transition"
        >
          <Plus className="w-4 h-4" />
          <span>Register New API</span>
        </Link>
      </div>

      {/* KPI Overview Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Total Gateway Calls
          </span>
          <div className="text-3xl font-extrabold text-white mt-1">
            {Number(overview?.totalRequests || 0).toLocaleString()}
          </div>
          <span className="text-xs text-emerald-400 mt-2 block">
            {Number(overview?.successfulRequests || 0).toLocaleString()} Successful (2xx)
          </span>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            p95 Latency
          </span>
          <div className="text-3xl font-extrabold text-white mt-1">
            {overview?.p95LatencyMs || 0}ms
          </div>
          <span className="text-xs text-slate-400 mt-2 block">
            Avg: {overview?.averageLatencyMs || 0}ms
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
            {overview?.errorCount || 0} Total Failures
          </span>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            My Registered APIs
          </span>
          <div className="text-3xl font-extrabold text-white mt-1">
            {apis.length}
          </div>
          <Link to="/owner/apis" className="text-xs text-indigo-400 hover:underline mt-2 block">
            View API endpoints →
          </Link>
        </div>
      </div>

      {/* APIs Table */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <Layers className="w-4 h-4 text-indigo-400" />
            <span>My Registered APIs</span>
          </h2>
          <Link to="/owner/apis" className="text-xs text-indigo-400 hover:underline">
            Manage All
          </Link>
        </div>

        {apis.length === 0 ? (
          <div className="text-center py-16 text-slate-400 text-xs">
            No APIs registered yet. Click "Register New API" to get started.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-mono">
                  <th className="pb-3 font-normal">API Name</th>
                  <th className="pb-3 font-normal">Slug & Route</th>
                  <th className="pb-3 font-normal">Status</th>
                  <th className="pb-3 font-normal">Endpoints</th>
                  <th className="pb-3 font-normal">Plans</th>
                  <th className="pb-3 font-normal text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {apis.map((api) => (
                  <tr key={api.id} className="hover:bg-slate-850/50">
                    <td className="py-3 font-sans font-bold text-white">{api.name}</td>
                    <td className="py-3 text-indigo-300">/gateway/{api.slug}/**</td>
                    <td className="py-3">
                      <StatusBadge status={api.status} />
                    </td>
                    <td className="py-3 text-slate-400">{api.endpoints?.length || 1}</td>
                    <td className="py-3 text-slate-400">{api.plans?.length || 1}</td>
                    <td className="py-3 text-right font-sans">
                      <Link
                        to="/owner/analytics"
                        className="text-xs text-indigo-400 hover:text-white mr-3"
                      >
                        Analytics
                      </Link>
                      <Link
                        to="/owner/apis"
                        className="text-xs text-slate-300 hover:text-white"
                      >
                        Manage
                      </Link>
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
