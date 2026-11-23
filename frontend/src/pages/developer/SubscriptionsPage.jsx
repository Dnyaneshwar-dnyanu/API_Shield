import React, { useState, useEffect } from 'react';
import { subscriptionApi } from '../../api/subscriptionApi';
import { CreditCard, Trash2, AlertCircle, CheckCircle } from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';

export const SubscriptionsPage = () => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionError, setActionError] = useState(null);

  const fetchSubscriptions = () => {
    setLoading(true);
    subscriptionApi.getMySubscriptions({ page: 0, size: 50 })
      .then((data) => setSubscriptions(data.content || []))
      .catch(() => setSubscriptions([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchSubscriptions();
  }, []);

  const handleCancel = async (id) => {
    if (!window.confirm('Are you sure you want to cancel this subscription?')) return;
    try {
      await subscriptionApi.cancelSubscription(id);
      fetchSubscriptions();
    } catch (err) {
      setActionError(err.response?.data?.detail || 'Failed to cancel subscription');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
      <div className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">My Subscriptions</h1>
          <p className="text-slate-400 text-sm mt-1">
            Review your active plan tiers, billing intervals, and quota limits.
          </p>
        </div>
      </div>

      {actionError && (
        <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{actionError}</span>
        </div>
      )}

      {loading ? (
        <div className="py-20 flex justify-center">
          <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
        </div>
      ) : subscriptions.length === 0 ? (
        <div className="text-center py-20 rounded-2xl bg-slate-900 border border-dashed border-slate-800">
          <CreditCard className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-bold text-white">No Active Subscriptions</h3>
          <p className="text-xs text-slate-400 mt-1">Browse the API Marketplace to subscribe to plans.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {subscriptions.map((sub) => (
            <div
              key={sub.id}
              className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between space-y-4"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-mono text-slate-400">ID: {sub.id.substring(0, 8)}...</span>
                  <StatusBadge status={sub.status} />
                </div>

                <h3 className="text-lg font-bold text-white">{sub.apiName || 'API Subscription'}</h3>
                <p className="text-xs font-mono text-indigo-400 mt-0.5">Plan Tier: {sub.planName || sub.planId}</p>

                <div className="mt-4 pt-4 border-t border-slate-800/80 space-y-2 text-xs text-slate-300">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Monthly Quota:</span>
                    <span className="font-mono font-semibold text-white">
                      {sub.quotaMonthly ? Number(sub.quotaMonthly).toLocaleString() : 'Unlimited'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Subscribed On:</span>
                    <span className="font-mono text-slate-400">
                      {new Date(sub.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>

              {sub.status === 'ACTIVE' && (
                <button
                  onClick={() => handleCancel(sub.id)}
                  className="w-full py-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-semibold border border-rose-500/30 flex items-center justify-center space-x-1.5 transition"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Cancel Subscription</span>
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
