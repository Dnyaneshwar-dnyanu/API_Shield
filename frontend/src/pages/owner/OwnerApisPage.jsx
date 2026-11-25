import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ownerApi } from '../../api/ownerApi';
import { 
  Layers, 
  Plus, 
  Globe, 
  CheckCircle, 
  EyeOff, 
  ExternalLink,
  PlusCircle
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';

export const OwnerApisPage = () => {
  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(true);

  // Add Endpoint Modal state
  const [selectedApiForEndpoint, setSelectedApiForEndpoint] = useState(null);
  const [endpointPath, setEndpointPath] = useState('');
  const [endpointMethod, setEndpointMethod] = useState('GET');
  const [endpointRateLimit, setEndpointRateLimit] = useState(10);
  const [endpointSaving, setEndpointSaving] = useState(false);

  // Add Plan Modal state
  const [selectedApiForPlan, setSelectedApiForPlan] = useState(null);
  const [planName, setPlanName] = useState('Pro Tier');
  const [planTier, setPlanTier] = useState('PRO');
  const [monthlyPrice, setMonthlyPrice] = useState('49.00');
  const [monthlyQuota, setMonthlyQuota] = useState(50000);
  const [planRps, setPlanRps] = useState(50);
  const [planSaving, setPlanSaving] = useState(false);

  const fetchApis = () => {
    setLoading(true);
    ownerApi.getMyApis({ page: 0, size: 50 })
      .then((data) => setApis(data.content || []))
      .catch(() => setApis([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchApis();
  }, []);

  const handleTogglePublish = async (api) => {
    try {
      if (api.status === 'PUBLISHED') {
        await ownerApi.unpublishApi(api.id);
      } else {
        await ownerApi.publishApi(api.id);
      }
      fetchApis();
    } catch (err) {
      alert('Error updating API status: ' + (err.response?.data?.detail || err.message));
    }
  };

  const handleAddEndpointSubmit = async (e) => {
    e.preventDefault();
    setEndpointSaving(true);
    try {
      await ownerApi.addEndpoint(selectedApiForEndpoint.id, {
        pathPattern: endpointPath.startsWith('/') ? endpointPath : `/${endpointPath}`,
        httpMethod: endpointMethod,
        rateLimitRps: Number(endpointRateLimit),
        quotaCost: 1,
      });
      setSelectedApiForEndpoint(null);
      setEndpointPath('');
      fetchApis();
    } catch (err) {
      alert('Failed to add endpoint: ' + (err.response?.data?.detail || err.message));
    } finally {
      setEndpointSaving(false);
    }
  };

  const handleAddPlanSubmit = async (e) => {
    e.preventDefault();
    setPlanSaving(true);
    try {
      const priceInCents = Math.round((parseFloat(monthlyPrice) || 0) * 100);
      const resolvedPlanType = (planTier === 'PAID' || planTier === 'PRO') ? 'PAID' : 'FREE';

      await ownerApi.addPlan(selectedApiForPlan.id, {
        name: planName,
        planType: resolvedPlanType,
        tier: resolvedPlanType,
        priceCents: priceInCents,
        monthlyPrice: parseFloat(monthlyPrice) || 0,
        quotaMonthly: Number(monthlyQuota) || 5000,
        monthlyQuota: Number(monthlyQuota) || 5000,
        rateLimitRps: Number(planRps) || 10,
        billingCycle: 'MONTHLY',
      });
      setSelectedApiForPlan(null);
      fetchApis();
    } catch (err) {
      alert('Failed to add pricing plan: ' + (err.response?.data?.detail || err.message));
    } finally {
      setPlanSaving(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">API Portfolio</h1>
          <p className="text-slate-400 text-sm mt-1">
            Configure routes, endpoints, and monetization tiers for your backend services.
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

      {loading ? (
        <div className="py-20 flex justify-center">
          <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
        </div>
      ) : apis.length === 0 ? (
        <div className="text-center py-20 rounded-2xl bg-slate-900 border border-dashed border-slate-800">
          <Layers className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-bold text-white">No APIs Registered</h3>
          <p className="text-xs text-slate-400 mt-1">
            Register your first service to begin proxying traffic through APIShield.
          </p>
          <Link
            to="/owner/apis/new"
            className="mt-4 inline-flex items-center space-x-1.5 px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-semibold"
          >
            <Plus className="w-4 h-4" />
            <span>Register API Now</span>
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {apis.map((api) => (
            <div
              key={api.id}
              className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-6"
            >
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="text-xl font-bold text-white">{api.name}</h3>
                    <StatusBadge status={api.status} />
                    <span className="text-xs font-mono text-slate-400">v{api.version || '1.0'}</span>
                  </div>
                  <div className="flex items-center space-x-3 text-xs font-mono">
                    <span className="text-indigo-400">/gateway/{api.slug}/**</span>
                    <span className="text-slate-600">•</span>
                    <span className="text-slate-400">Target: {api.targetBaseUrl}</span>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => handleTogglePublish(api)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition ${
                      api.status === 'PUBLISHED'
                        ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30 hover:bg-amber-500/20'
                        : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/20'
                    }`}
                  >
                    {api.status === 'PUBLISHED' ? (
                      <>
                        <EyeOff className="w-3.5 h-3.5" />
                        <span>Unpublish</span>
                      </>
                    ) : (
                      <>
                        <CheckCircle className="w-3.5 h-3.5" />
                        <span>Publish to Catalog</span>
                      </>
                    )}
                  </button>

                  <button
                    onClick={() => setSelectedApiForEndpoint(api)}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-750 text-slate-200 text-xs font-semibold flex items-center space-x-1"
                  >
                    <PlusCircle className="w-3.5 h-3.5 text-indigo-400" />
                    <span>Add Endpoint</span>
                  </button>

                  <button
                    onClick={() => setSelectedApiForPlan(api)}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-750 text-slate-200 text-xs font-semibold flex items-center space-x-1"
                  >
                    <PlusCircle className="w-3.5 h-3.5 text-emerald-400" />
                    <span>Add Plan</span>
                  </button>
                </div>
              </div>

              {/* Endpoints & Plans Summary */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-slate-800/80">
                {/* Endpoints preview */}
                <div className="space-y-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Configured Endpoints ({api.endpoints?.length || 0})
                  </span>
                  <div className="space-y-1.5">
                    {api.endpoints && api.endpoints.length > 0 ? (
                      api.endpoints.map((ep, i) => (
                        <div key={i} className="p-2 rounded-lg bg-slate-950 text-xs font-mono flex items-center justify-between text-slate-300">
                          <span><b className="text-indigo-400">{ep.httpMethod}</b> {ep.pathPattern}</span>
                          <span className="text-slate-500">{ep.rateLimitRps} RPS</span>
                        </div>
                      ))
                    ) : (
                      <div className="text-xs text-slate-500">Default wildcard route /gateway/{api.slug}/** active.</div>
                    )}
                  </div>
                </div>

                {/* Plans preview */}
                <div className="space-y-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Pricing Plans ({api.plans?.length || 0})
                  </span>
                  <div className="space-y-1.5">
                    {api.plans && api.plans.length > 0 ? (
                      api.plans.map((p, i) => (
                        <div key={i} className="p-2 rounded-lg bg-slate-950 text-xs flex items-center justify-between text-slate-300">
                          <span className="font-semibold text-white">{p.name} ({p.tier})</span>
                          <span className="font-mono text-emerald-400">${p.monthlyPrice || '0.00'}/mo</span>
                        </div>
                      ))
                    ) : (
                      <div className="text-xs text-slate-500">No custom plans defined.</div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Endpoint Modal */}
      {selectedApiForEndpoint && (
        <Modal
          isOpen={!!selectedApiForEndpoint}
          onClose={() => setSelectedApiForEndpoint(null)}
          title={`Add Endpoint to ${selectedApiForEndpoint.name}`}
        >
          <form onSubmit={handleAddEndpointSubmit} className="space-y-4">
            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Method</label>
                <select
                  value={endpointMethod}
                  onChange={(e) => setEndpointMethod(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-white focus:outline-none"
                >
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="DELETE">DELETE</option>
                </select>
              </div>
              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-400 mb-1">Path Pattern</label>
                <input
                  type="text"
                  required
                  value={endpointPath}
                  onChange={(e) => setEndpointPath(e.target.value)}
                  placeholder="/current"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-white focus:outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">Rate Limit (RPS)</label>
              <input
                type="number"
                min="1"
                value={endpointRateLimit}
                onChange={(e) => setEndpointRateLimit(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-white focus:outline-none"
              />
            </div>

            <div className="pt-4 flex justify-end space-x-3">
              <button
                type="button"
                onClick={() => setSelectedApiForEndpoint(null)}
                className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={endpointSaving}
                className="px-5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold"
              >
                {endpointSaving ? 'Saving...' : 'Add Endpoint'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Add Plan Modal */}
      {selectedApiForPlan && (
        <Modal
          isOpen={!!selectedApiForPlan}
          onClose={() => setSelectedApiForPlan(null)}
          title={`Create Pricing Tier for ${selectedApiForPlan.name}`}
        >
          <form onSubmit={handleAddPlanSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">Plan Name</label>
              <input
                type="text"
                required
                value={planName}
                onChange={(e) => setPlanName(e.target.value)}
                placeholder="Pro Plan"
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Plan Tier</label>
                <select
                  value={planTier}
                  onChange={(e) => setPlanTier(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none"
                >
                  <option value="FREE">FREE</option>
                  <option value="PRO">PRO</option>
                  <option value="ENTERPRISE">ENTERPRISE</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Monthly Price ($)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={monthlyPrice}
                  onChange={(e) => setMonthlyPrice(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Monthly Quota</label>
                <input
                  type="number"
                  min="100"
                  value={monthlyQuota}
                  onChange={(e) => setMonthlyQuota(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Burst RPS Limit</label>
                <input
                  type="number"
                  min="1"
                  value={planRps}
                  onChange={(e) => setPlanRps(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white focus:outline-none"
                />
              </div>
            </div>

            <div className="pt-4 flex justify-end space-x-3">
              <button
                type="button"
                onClick={() => setSelectedApiForPlan(null)}
                className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={planSaving}
                className="px-5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold"
              >
                {planSaving ? 'Saving...' : 'Create Plan'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
