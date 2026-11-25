import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { catalogApi } from '../../api/catalogApi';
import { subscriptionApi } from '../../api/subscriptionApi';
import { useAuth } from '../../context/AuthContext';
import { 
  Shield, 
  Terminal, 
  Layers, 
  Check, 
  ArrowLeft, 
  Copy, 
  Zap, 
  CreditCard,
  ExternalLink,
  Code2
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';

export const ApiDetailPage = () => {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, isDeveloper } = useAuth();

  const [api, setApi] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('endpoints'); // 'endpoints' | 'plans' | 'code'
  const [copiedSnippet, setCopiedSnippet] = useState(false);

  // Subscription Modal state
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [subscribing, setSubscribing] = useState(false);
  const [subSuccess, setSubSuccess] = useState(null);
  const [subError, setSubError] = useState(null);

  useEffect(() => {
    catalogApi.getCatalogDetail(slug)
      .then((data) => setApi(data))
      .catch(() => setApi(null))
      .finally(() => setLoading(false));
  }, [slug]);

  const handleSubscribe = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (!isDeveloper) {
      setSubError('Only registered developer accounts can subscribe to APIs.');
      return;
    }

    setSubscribing(true);
    setSubError(null);
    try {
      const result = await subscriptionApi.subscribe({
        apiId: api.id,
        planId: selectedPlan.id,
      });
      setSubSuccess(result);
    } catch (err) {
      setSubError(err.response?.data?.detail || 'Failed to initiate subscription');
    } finally {
      setSubscribing(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 flex justify-center">
        <div className="w-10 h-10 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
      </div>
    );
  }

  if (!api) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-24 text-center">
        <h2 className="text-2xl font-bold text-white">API Not Found</h2>
        <p className="text-slate-400 text-sm mt-2">The API specification you requested does not exist.</p>
        <button
          onClick={() => navigate('/marketplace')}
          className="mt-6 px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-semibold"
        >
          Back to Marketplace
        </button>
      </div>
    );
  }

  const sampleCurl = `curl -X GET "https://api.apishield.io/gateway/${api.slug}/current" \\
  -H "X-API-Key: ash_live_your_api_key_here" \\
  -H "Accept: application/json"`;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Back button */}
      <button
        onClick={() => navigate('/marketplace')}
        className="inline-flex items-center space-x-1.5 text-xs font-semibold text-slate-400 hover:text-white mb-6 transition"
      >
        <ArrowLeft className="w-3.5 h-3.5" />
        <span>Back to Catalog</span>
      </button>

      {/* Header Banner */}
      <div className="p-8 rounded-3xl bg-slate-900 border border-slate-800 relative overflow-hidden mb-8">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-6">
          <div className="space-y-3">
            <div className="flex items-center space-x-3">
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                {api.category || 'General'}
              </span>
              <StatusBadge status="PUBLISHED" />
              <span className="text-xs font-mono text-slate-400">v{api.version || '1.0'}</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              {api.name}
            </h1>
            <p className="text-slate-300 text-sm max-w-2xl leading-relaxed">
              {api.description || 'Enterprise API ready for integration.'}
            </p>
            <div className="flex items-center space-x-2 text-xs font-mono text-emerald-400 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 w-fit">
              <span>GATEWAY ENDPOINT:</span>
              <span className="text-white">/gateway/{api.slug}/**</span>
            </div>
          </div>

          {/* Quick Subscribe CTA */}
          <div className="flex flex-col sm:flex-row md:flex-col gap-3">
            <button
              onClick={() => setActiveTab('plans')}
              className="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 flex items-center justify-center space-x-2 transition"
            >
              <span>Subscribe to API</span>
              <Zap className="w-4 h-4" />
            </button>
            {isAuthenticated && isDeveloper && (
              <button
                onClick={() => navigate('/dev/playground')}
                className="px-6 py-3 rounded-xl bg-slate-800 hover:bg-slate-750 text-slate-200 border border-slate-700 font-semibold flex items-center justify-center space-x-2 transition text-sm"
              >
                <Terminal className="w-4 h-4" />
                <span>Open Playground</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center space-x-3 border-b border-slate-800 pb-px mb-8">
        <button
          onClick={() => setActiveTab('endpoints')}
          className={`pb-3 text-sm font-semibold border-b-2 transition flex items-center space-x-2 ${
            activeTab === 'endpoints'
              ? 'border-indigo-500 text-indigo-400'
              : 'border-transparent text-slate-400 hover:text-white'
          }`}
        >
          <Terminal className="w-4 h-4" />
          <span>Endpoints ({api.endpoints?.length || 0})</span>
        </button>

        <button
          onClick={() => setActiveTab('plans')}
          className={`pb-3 text-sm font-semibold border-b-2 transition flex items-center space-x-2 ${
            activeTab === 'plans'
              ? 'border-indigo-500 text-indigo-400'
              : 'border-transparent text-slate-400 hover:text-white'
          }`}
        >
          <CreditCard className="w-4 h-4" />
          <span>Pricing & Quotas ({api.plans?.length || 0})</span>
        </button>

        <button
          onClick={() => setActiveTab('code')}
          className={`pb-3 text-sm font-semibold border-b-2 transition flex items-center space-x-2 ${
            activeTab === 'code'
              ? 'border-indigo-500 text-indigo-400'
              : 'border-transparent text-slate-400 hover:text-white'
          }`}
        >
          <Code2 className="w-4 h-4" />
          <span>Code Examples</span>
        </button>
      </div>

      {/* Tab 1: Endpoints */}
      {activeTab === 'endpoints' && (
        <div className="space-y-4">
          {api.endpoints && api.endpoints.length > 0 ? (
            api.endpoints.map((ep, idx) => (
              <div
                key={ep.id || idx}
                className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="flex items-center space-x-3">
                  <span className={`px-2.5 py-1 rounded text-xs font-mono font-bold ${
                    ep.httpMethod === 'GET'
                      ? 'bg-sky-500/10 text-sky-400 border border-sky-500/30'
                      : ep.httpMethod === 'POST'
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                      : ep.httpMethod === 'DELETE'
                      ? 'bg-rose-500/10 text-rose-400 border border-rose-500/30'
                      : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                  }`}>
                    {ep.httpMethod}
                  </span>
                  <div>
                    <div className="text-sm font-mono font-semibold text-white">
                      /gateway/{api.slug}{ep.pathPattern}
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">
                      {ep.description || 'Configured downstream route'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center space-x-4 text-xs font-mono text-slate-400">
                  {ep.rateLimitRps && (
                    <span>Rate Limit: {ep.rateLimitRps} RPS</span>
                  )}
                  {ep.quotaCost && (
                    <span>Cost: {ep.quotaCost} unit(s)</span>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div className="p-8 text-center rounded-xl bg-slate-900 border border-slate-800 text-slate-400 text-sm">
              All routes under <code className="text-indigo-300">/gateway/{api.slug}/**</code> are enabled.
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Pricing Plans */}
      {activeTab === 'plans' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {api.plans && api.plans.length > 0 ? (
            api.plans.map((plan) => (
              <div
                key={plan.id}
                className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between hover:border-indigo-500/50 transition relative"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-xl font-bold text-white">{plan.name}</h3>
                    <StatusBadge status={plan.tier || 'PLAN'} />
                  </div>

                  <div className="mb-6">
                    <span className="text-3xl font-extrabold text-white">
                      ${plan.monthlyPrice || '0.00'}
                    </span>
                    <span className="text-xs text-slate-400 ml-1">/ month</span>
                  </div>

                  <ul className="space-y-3 text-xs text-slate-300">
                    <li className="flex items-center space-x-2">
                      <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      <span>{Number(plan.monthlyQuota || plan.quotaMonthly || 1000).toLocaleString()} requests/month</span>
                    </li>
                    <li className="flex items-center space-x-2">
                      <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      <span>{plan.rateLimitRps || 10} requests/second burst limit</span>
                    </li>
                    <li className="flex items-center space-x-2">
                      <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      <span>Token Bucket Redis sliding window</span>
                    </li>
                    <li className="flex items-center space-x-2">
                      <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      <span>Cryptographic API Key generation</span>
                    </li>
                  </ul>
                </div>

                <button
                  onClick={() => {
                    setSelectedPlan(plan);
                    setSubSuccess(null);
                    setSubError(null);
                  }}
                  className="w-full mt-8 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs shadow-md transition"
                >
                  Select {plan.name}
                </button>
              </div>
            ))
          ) : (
            <div className="col-span-3 text-center py-12 text-slate-400">
              No specific plans published yet. Default access tier applies.
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Code Examples */}
      {activeTab === 'code' && (
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono text-slate-400 uppercase tracking-wider">cURL Request</span>
            <button
              onClick={() => {
                navigator.clipboard.writeText(sampleCurl);
                setCopiedSnippet(true);
                setTimeout(() => setCopiedSnippet(false), 2000);
              }}
              className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 text-xs text-slate-300 flex items-center space-x-1"
            >
              {copiedSnippet ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedSnippet ? 'Copied!' : 'Copy cURL'}</span>
            </button>
          </div>
          <pre className="p-4 rounded-xl bg-slate-950 border border-slate-850 text-xs font-mono text-slate-200 overflow-x-auto">
            {sampleCurl}
          </pre>
        </div>
      )}

      {/* Plan Subscription Modal */}
      {selectedPlan && (
        <Modal
          isOpen={!!selectedPlan}
          onClose={() => setSelectedPlan(null)}
          title={`Subscribe to ${selectedPlan.name}`}
        >
          {subSuccess ? (
            <div className="space-y-4 text-center py-4">
              <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mx-auto">
                <Check className="w-6 h-6" />
              </div>
              <h4 className="text-lg font-bold text-white">Subscription Active!</h4>
              <p className="text-xs text-slate-400">
                You have successfully subscribed to {api.name} under the {selectedPlan.name} plan.
              </p>
              <div className="pt-4 flex justify-center space-x-3">
                <button
                  onClick={() => navigate('/dev/keys')}
                  className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs transition"
                >
                  Generate API Key
                </button>
                <button
                  onClick={() => setSelectedPlan(null)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold"
                >
                  Close
                </button>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-slate-850 border border-slate-800 text-xs space-y-1">
                <div className="flex justify-between">
                  <span className="text-slate-400">Target API:</span>
                  <span className="font-semibold text-white">{api.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Plan Tier:</span>
                  <span className="font-semibold text-indigo-400">{selectedPlan.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Monthly Quota:</span>
                  <span className="font-semibold text-white">{Number(selectedPlan.monthlyQuota || selectedPlan.quotaMonthly || 0).toLocaleString()} calls</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Price:</span>
                  <span className="font-bold text-emerald-400">${selectedPlan.monthlyPrice || '0.00'}/month</span>
                </div>
              </div>

              {subError && (
                <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
                  {subError}
                </div>
              )}

              <div className="pt-4 flex justify-end space-x-3">
                <button
                  onClick={() => setSelectedPlan(null)}
                  className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSubscribe}
                  disabled={subscribing}
                  className="px-5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md disabled:opacity-50 flex items-center space-x-1.5"
                >
                  {subscribing ? (
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <Zap className="w-3.5 h-3.5" />
                      <span>Confirm Subscription</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          )}
        </Modal>
      )}
    </div>
  );
};
