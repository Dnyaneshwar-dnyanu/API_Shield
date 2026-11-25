import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ownerApi } from '../../api/ownerApi';
import { 
  Layers, 
  ArrowRight, 
  Globe, 
  AlertCircle, 
  Check, 
  ShieldAlert
} from 'lucide-react';

export const CreateApiPage = () => {
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [description, setDescription] = useState('');
  const [targetBaseUrl, setTargetBaseUrl] = useState('');
  const [category, setCategory] = useState('Developer Tools');
  const [version, setVersion] = useState('1.0');
  const [authType, setAuthType] = useState('API_KEY');

  // Initial Endpoint
  const [pathPattern, setPathPattern] = useState('/current');
  const [httpMethod, setHttpMethod] = useState('GET');
  const [rateLimitRps, setRateLimitRps] = useState(20);

  // Initial Pricing Plan
  const [planName, setPlanName] = useState('Free Tier');
  const [planTier, setPlanTier] = useState('FREE');
  const [monthlyPrice, setMonthlyPrice] = useState('0.00');
  const [monthlyQuota, setMonthlyQuota] = useState(5000);

  const [publishNow, setPublishNow] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleNameChange = (val) => {
    setName(val);
    if (!slug) {
      setSlug(val.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // 1. Create API
      const newApi = await ownerApi.createApi({
        name,
        slug,
        description,
        targetBackendUrl: targetBaseUrl,
        targetBaseUrl,
        category,
        versionLabel: version,
        version,
        authType,
      });

      // 2. Add Initial Endpoint
      if (pathPattern) {
        await ownerApi.addEndpoint(newApi.id, {
          pathPattern: pathPattern.startsWith('/') ? pathPattern : `/${pathPattern}`,
          httpMethod,
          rateLimitRps: Number(rateLimitRps),
          quotaCost: 1,
        });
      }

      // 3. Add Initial Plan
      await ownerApi.addPlan(newApi.id, {
        name: planName,
        tier: planTier,
        monthlyPrice: parseFloat(monthlyPrice),
        monthlyQuota: Number(monthlyQuota),
        rateLimitRps: Number(rateLimitRps),
      });

      // 4. Publish if requested
      if (publishNow) {
        await ownerApi.publishApi(newApi.id);
      }

      navigate('/owner/apis');
    } catch (err) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Failed to register API');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      <div className="pb-6 border-b border-slate-800">
        <h1 className="text-3xl font-extrabold text-white tracking-tight">Register New API</h1>
        <p className="text-slate-400 text-sm mt-1">
          Connect your upstream backend target and configure edge routing through Spring Cloud Gateway.
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Step 1: Basic Specifications */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <Layers className="w-4 h-4 text-indigo-400" />
            <span>Service Metadata</span>
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">API Name</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => handleNameChange(e.target.value)}
                placeholder="e.g. Weather Forecast API"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Gateway Slug</label>
              <input
                type="text"
                required
                value={slug}
                onChange={(e) => setSlug(e.target.value)}
                placeholder="e.g. weather-forecast"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-indigo-300 focus:outline-none focus:border-indigo-500"
              />
              <span className="text-[10px] text-slate-500 mt-1 block">
                Proxied at: <code className="text-slate-300">/gateway/{slug || '{slug}'}/**</code>
              </span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Target Base URL</label>
            <input
              type="url"
              required
              value={targetBaseUrl}
              onChange={(e) => setTargetBaseUrl(e.target.value)}
              placeholder="https://api.example.com/v1"
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm font-mono text-emerald-400 focus:outline-none focus:border-indigo-500"
            />
            <span className="text-[10px] text-slate-500 mt-1 block">
              Protected against SSRF. Private IPs (127.0.0.1, 10.x, 192.168.x) are strictly verified.
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                <option value="AI & ML">AI & ML</option>
                <option value="Finance & Crypto">Finance & Crypto</option>
                <option value="Weather & Data">Weather & Data</option>
                <option value="E-Commerce">E-Commerce</option>
                <option value="Security & Auth">Security & Auth</option>
                <option value="Developer Tools">Developer Tools</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">API Description</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="High-availability real-time forecast data"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>
        </div>

        {/* Step 2: Initial Endpoint */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <Globe className="w-4 h-4 text-emerald-400" />
            <span>Default Endpoint Configuration</span>
          </h2>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">HTTP Verb</label>
              <select
                value={httpMethod}
                onChange={(e) => setHttpMethod(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono font-bold text-white"
              >
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold text-slate-300 mb-1">Path Pattern</label>
              <input
                type="text"
                value={pathPattern}
                onChange={(e) => setPathPattern(e.target.value)}
                placeholder="/current"
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-white"
              />
            </div>
          </div>
        </div>

        {/* Step 3: Initial Monetization Plan */}
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <Check className="w-4 h-4 text-sky-400" />
            <span>Default Pricing & Quota Tier</span>
          </h2>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Plan Name</label>
              <input
                type="text"
                value={planName}
                onChange={(e) => setPlanName(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Tier</label>
              <select
                value={planTier}
                onChange={(e) => setPlanTier(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white"
              >
                <option value="FREE">FREE</option>
                <option value="PRO">PRO</option>
                <option value="ENTERPRISE">ENTERPRISE</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Monthly Price ($)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={monthlyPrice}
                onChange={(e) => setMonthlyPrice(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Monthly Quota</label>
              <input
                type="number"
                min="100"
                value={monthlyQuota}
                onChange={(e) => setMonthlyQuota(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-white font-mono"
              />
            </div>
          </div>
        </div>

        {/* Publish Checkbox */}
        <div className="flex items-center space-x-3 p-4 rounded-xl bg-slate-900 border border-slate-800">
          <input
            type="checkbox"
            id="publishNow"
            checked={publishNow}
            onChange={(e) => setPublishNow(e.target.checked)}
            className="w-4 h-4 rounded text-indigo-600 focus:ring-0 bg-slate-950 border-slate-700"
          />
          <label htmlFor="publishNow" className="text-xs text-slate-300 cursor-pointer">
            Publish immediately to the public API Marketplace catalog
          </label>
        </div>

        {/* Submit */}
        <div className="flex justify-end space-x-4">
          <button
            type="button"
            onClick={() => navigate('/owner/apis')}
            className="px-6 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-750"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-8 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs shadow-lg shadow-indigo-600/30 flex items-center space-x-2 transition disabled:opacity-50"
          >
            {loading ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <>
                <span>Publish API to Gateway</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
