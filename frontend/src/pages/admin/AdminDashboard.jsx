import React, { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { 
  Settings, 
  Server, 
  Users, 
  ShieldAlert, 
  CheckCircle, 
  Slash,
  RefreshCw
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';

const MICROSERVICES = [
  { name: 'Edge API Gateway', port: 8080, type: 'Data Plane', status: 'UP' },
  { name: 'Discovery Server (Eureka)', port: 8761, type: 'Registry', status: 'UP' },
  { name: 'User & Auth Service', port: 8081, type: 'Control Plane', status: 'UP' },
  { name: 'API Registry Service', port: 8082, type: 'Control Plane', status: 'UP' },
  { name: 'Subscription Service', port: 8083, type: 'Control Plane', status: 'UP' },
  { name: 'API Key Service', port: 8084, type: 'Control Plane', status: 'UP' },
  { name: 'Payment Service', port: 8085, type: 'Control Plane', status: 'UP' },
  { name: 'Notification Service', port: 8086, type: 'Control Plane', status: 'UP' },
  { name: 'Analytics Service', port: 8087, type: 'Control Plane', status: 'UP' },
];

export const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = () => {
    setLoading(true);
    adminApi.getUsers({ page: 0, size: 50 })
      .then((data) => setUsers(data.content || []))
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleSuspend = async (userId) => {
    const reason = prompt('Enter suspension reason:', 'Terms of service violation');
    if (!reason) return;
    try {
      await adminApi.suspendUser(userId, reason);
      fetchUsers();
    } catch (err) {
      alert('Error suspending user: ' + (err.response?.data?.detail || err.message));
    }
  };

  const handleReactivate = async (userId) => {
    if (!window.confirm('Reactivate this user account?')) return;
    try {
      await adminApi.reactivateUser(userId);
      fetchUsers();
    } catch (err) {
      alert('Error reactivating user: ' + (err.response?.data?.detail || err.message));
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Header */}
      <div className="pb-6 border-b border-slate-800">
        <h1 className="text-3xl font-extrabold text-white tracking-tight flex items-center space-x-2">
          <Settings className="w-8 h-8 text-amber-400" />
          <span>Platform Administrator Console</span>
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          System telemetry, microservices topology, and user access management.
        </p>
      </div>

      {/* Microservices Topology Grid */}
      <div className="space-y-4">
        <h2 className="text-sm font-bold uppercase tracking-wider text-slate-300 flex items-center space-x-2">
          <Server className="w-4 h-4 text-emerald-400" />
          <span>Microservices Infrastructure Topology</span>
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {MICROSERVICES.map((ms) => (
            <div
              key={ms.port}
              className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between"
            >
              <div>
                <h4 className="text-sm font-bold text-white">{ms.name}</h4>
                <div className="flex items-center space-x-2 text-xs font-mono text-slate-400 mt-1">
                  <span>Port: {ms.port}</span>
                  <span>•</span>
                  <span className="text-indigo-400">{ms.type}</span>
                </div>
              </div>
              <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5 animate-pulse" />
                {ms.status}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* User Governance Table */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <Users className="w-4 h-4 text-indigo-400" />
            <span>Platform User Directory</span>
          </h2>
          <button
            onClick={fetchUsers}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
            title="Refresh Users"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {loading ? (
          <div className="py-16 flex justify-center">
            <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
          </div>
        ) : users.length === 0 ? (
          <div className="py-12 text-center text-slate-400 text-xs">
            No registered users found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-mono">
                  <th className="pb-3 font-normal">User Name</th>
                  <th className="pb-3 font-normal">Email Address</th>
                  <th className="pb-3 font-normal">Platform Role</th>
                  <th className="pb-3 font-normal">Status</th>
                  <th className="pb-3 font-normal text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-850/50">
                    <td className="py-3 font-sans font-bold text-white">{u.name || 'User'}</td>
                    <td className="py-3 text-slate-300">{u.email}</td>
                    <td className="py-3 text-indigo-400">{u.role}</td>
                    <td className="py-3">
                      <StatusBadge status={u.status || 'ACTIVE'} />
                    </td>
                    <td className="py-3 text-right font-sans">
                      {u.status === 'SUSPENDED' ? (
                        <button
                          onClick={() => handleReactivate(u.id)}
                          className="px-2.5 py-1 rounded bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-semibold"
                        >
                          Reactivate
                        </button>
                      ) : (
                        <button
                          onClick={() => handleSuspend(u.id)}
                          className="px-2.5 py-1 rounded bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs font-semibold"
                        >
                          Suspend
                        </button>
                      )}
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
