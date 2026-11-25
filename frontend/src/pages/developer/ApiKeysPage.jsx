import React, { useState, useEffect } from 'react';
import { apiKeyApi } from '../../api/apiKeyApi';
import { subscriptionApi } from '../../api/subscriptionApi';
import { 
  Key, 
  Plus, 
  Trash2, 
  Copy, 
  Check, 
  AlertTriangle, 
  Lock,
  ShieldCheck,
  Clock
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';

export const ApiKeysPage = () => {
  const [keys, setKeys] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Generate Key Modal
  const [isGenerateOpen, setIsGenerateOpen] = useState(false);
  const [keyName, setKeyName] = useState('');
  const [selectedSubId, setSelectedSubId] = useState('');
  const [generating, setGenerating] = useState(false);
  const [generateError, setGenerateError] = useState(null);

  // Single-view raw key modal
  const [rawKeyData, setRawKeyData] = useState(null);
  const [copiedKey, setCopiedKey] = useState(false);

  const fetchKeys = () => {
    setLoading(true);
    apiKeyApi.getMyKeys({ page: 0, size: 50 })
      .then((data) => setKeys(data.content || []))
      .catch(() => setKeys([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchKeys();
    subscriptionApi.getMySubscriptions({ page: 0, size: 50 })
      .then((data) => {
        const subs = data.content || [];
        setSubscriptions(subs);
        if (subs.length > 0) setSelectedSubId(subs[0].id);
      })
      .catch(() => setSubscriptions([]));
  }, []);

  const handleGenerateSubmit = async (e) => {
    e.preventDefault();
    setGenerating(true);
    setGenerateError(null);

    try {
      const selectedSub = subscriptions.find((s) => s.id === selectedSubId);
      const data = await apiKeyApi.generateKey({
        name: keyName,
        subscriptionId: selectedSubId,
        apiId: selectedSub?.apiId || '',
      });

      setIsGenerateOpen(false);
      setKeyName('');
      setRawKeyData(data); // Display unmasked key in reveal-once modal
      fetchKeys();
    } catch (err) {
      setGenerateError(err.response?.data?.message || err.response?.data?.detail || 'Failed to generate API key');
    } finally {
      setGenerating(false);
    }
  };

  const handleRevoke = async (id) => {
    if (!window.confirm('Are you sure you want to revoke this API key? This action is permanent and cannot be undone.')) return;
    try {
      await apiKeyApi.revokeKey(id, 'User manually revoked from portal');
      fetchKeys();
    } catch (err) {
      alert('Failed to revoke API key: ' + (err.response?.data?.detail || err.message));
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">API Key Management</h1>
          <p className="text-slate-400 text-sm mt-1">
            Generate and govern cryptographically hashed API keys for edge gateway authentication.
          </p>
        </div>
        <button
          onClick={() => {
            setIsGenerateOpen(true);
            setGenerateError(null);
          }}
          className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs shadow-md shadow-indigo-600/30 flex items-center space-x-2 transition"
        >
          <Plus className="w-4 h-4" />
          <span>Generate New API Key</span>
        </button>
      </div>

      {loading ? (
        <div className="py-20 flex justify-center">
          <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
        </div>
      ) : keys.length === 0 ? (
        <div className="text-center py-20 rounded-2xl bg-slate-900 border border-dashed border-slate-800">
          <Key className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-base font-bold text-white">No API Keys Generated</h3>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            Create an API key to authenticate requests against <code className="text-indigo-400">/gateway/{'{slug}'}/**</code>.
          </p>
        </div>
      ) : (
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-mono">
                  <th className="pb-3 font-normal">Key Name</th>
                  <th className="pb-3 font-normal">Key Identifier</th>
                  <th className="pb-3 font-normal">Status</th>
                  <th className="pb-3 font-normal">Burst RPS</th>
                  <th className="pb-3 font-normal">Created</th>
                  <th className="pb-3 font-normal text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {keys.map((k) => (
                  <tr key={k.id} className="hover:bg-slate-850/50">
                    <td className="py-3 font-sans font-bold text-white">{k.name}</td>
                    <td className="py-3 text-indigo-300">
                      ash_live_{k.keyPrefix}_••••••••••••
                    </td>
                    <td className="py-3">
                      <StatusBadge status={k.status} />
                    </td>
                    <td className="py-3 text-slate-400">{k.rateLimitRps || 10} RPS</td>
                    <td className="py-3 text-slate-500">
                      {new Date(k.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3 text-right">
                      {k.status === 'ACTIVE' && (
                        <button
                          onClick={() => handleRevoke(k.id)}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
                          title="Revoke Key"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Generate Key Form Modal */}
      <Modal
        isOpen={isGenerateOpen}
        onClose={() => setIsGenerateOpen(false)}
        title="Generate Cryptographic API Key"
      >
        <form onSubmit={handleGenerateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Key Name / Label
            </label>
            <input
              type="text"
              required
              value={keyName}
              onChange={(e) => setKeyName(e.target.value)}
              placeholder="e.g. Production Weather Key"
              className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Associated Subscription
            </label>
            <select
              value={selectedSubId}
              onChange={(e) => setSelectedSubId(e.target.value)}
              className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              {subscriptions.map((sub) => (
                <option key={sub.id} value={sub.id}>
                  {sub.apiName || 'API'} — {sub.planName || 'Plan'} ({sub.id.substring(0, 8)}...)
                </option>
              ))}
            </select>
          </div>

          {generateError && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
              {generateError}
            </div>
          )}

          <div className="pt-4 flex justify-end space-x-3">
            <button
              type="button"
              onClick={() => setIsGenerateOpen(false)}
              className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={generating || subscriptions.length === 0}
              className="px-5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md disabled:opacity-50"
            >
              {generating ? 'Generating...' : 'Create Key'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Single-View Raw Key Reveal Modal */}
      {rawKeyData && (
        <Modal
          isOpen={!!rawKeyData}
          onClose={() => setRawKeyData(null)}
          title="Save Your API Key"
          maxWidth="max-w-xl"
        >
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-start space-x-3">
              <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-bold">Security Notice: Key Stored As SHA-256 Digest</p>
                <p className="mt-1 text-slate-300">
                  Copy your key right now. For your security, this raw key is never stored in plaintext and cannot be shown again.
                </p>
              </div>
            </div>

            <div>
              <label className="block text-xs font-mono text-slate-400 uppercase tracking-wider mb-1.5">
                Raw Secret Key
              </label>
              <div className="flex items-center space-x-2">
                <input
                  type="text"
                  readOnly
                  value={rawKeyData.apiKey || rawKeyData.rawApiKey || rawKeyData.key || ''}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-emerald-400 selection:bg-emerald-500 selection:text-black focus:outline-none"
                />
                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard.writeText(rawKeyData.apiKey || rawKeyData.rawApiKey || rawKeyData.key || '');
                    setCopiedKey(true);
                    setTimeout(() => setCopiedKey(false), 2000);
                  }}
                  className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold flex items-center space-x-1.5 transition"
                >
                  {copiedKey ? <Check className="w-4 h-4 text-emerald-300" /> : <Copy className="w-4 h-4" />}
                  <span>{copiedKey ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
            </div>

            <div className="pt-4 flex justify-end">
              <button
                type="button"
                onClick={() => setRawKeyData(null)}
                className="px-6 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-white text-xs font-semibold transition"
              >
                I Have Copied My Key Safely
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
