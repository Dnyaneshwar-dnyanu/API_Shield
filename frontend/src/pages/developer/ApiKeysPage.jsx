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
  Clock,
  RefreshCw
} from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Modal } from '../../components/common/Modal';
import { notify } from '../../utils/toast';

const STORAGE_KEY = 'apishield_developer_keys';

function getStoredKeys() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

function saveClientKey(id, prefix, rawKey) {
  try {
    const stored = getStoredKeys();
    if (id) stored[id] = rawKey;
    if (prefix) stored[prefix] = rawKey;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  } catch {}
}

export const ApiKeysPage = () => {
  const [keys, setKeys] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [localKeys, setLocalKeys] = useState(getStoredKeys());
  const [copiedId, setCopiedId] = useState(null);
  const [rollingId, setRollingId] = useState(null);

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
      .then((data) => setKeys(Array.isArray(data) ? data : (data?.content || [])))
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
      const activeSubId = selectedSubId || (subscriptions.length > 0 ? subscriptions[0].id : '');
      const selectedSub = subscriptions.find((s) => s.id === activeSubId);
      if (!selectedSub) {
        setGenerateError('Please select a valid active subscription.');
        setGenerating(false);
        return;
      }

      const data = await apiKeyApi.generateKey({
        name: keyName.trim(),
        subscriptionId: selectedSub.id,
        apiId: selectedSub.apiId || '',
      });

      const rawSecret = data.apiKey || data.rawApiKey || data.key || '';
      if (rawSecret) {
        saveClientKey(data.id, data.keyPrefix, rawSecret);
        setLocalKeys(getStoredKeys());
      }

      setIsGenerateOpen(false);
      setKeyName('');
      setRawKeyData(data); // Display unmasked key in reveal-once modal
      notify.success('API key generated! Please copy and store it safely.');
      fetchKeys();
    } catch (err) {
      let msg = err.response?.data?.detail || err.response?.data?.message || 'Failed to generate API key';
      if (err.response?.data?.errors && typeof err.response.data.errors === 'object') {
        const fieldMsgs = Object.values(err.response.data.errors);
        if (fieldMsgs.length > 0) {
          msg = fieldMsgs.join('; ');
        }
      }
      setGenerateError(msg);
      notify.error(err, msg);
    } finally {
      setGenerating(false);
    }
  };

  const handleCopyKey = (rawKey, keyId) => {
    if (!rawKey) return;
    navigator.clipboard.writeText(rawKey);
    setCopiedId(keyId);
    notify.success('API key copied to clipboard!');
    setTimeout(() => setCopiedId(null), 2500);
  };

  const handleRotateKey = async (id) => {
    if (!window.confirm('Rotate this API key? A new secret will be generated and the old one will be revoked immediately.')) return;
    setRollingId(id);
    try {
      const data = await apiKeyApi.rollKey(id);
      const rawSecret = data.apiKey || data.rawApiKey || data.key || '';
      if (rawSecret) {
        saveClientKey(data.id || id, data.keyPrefix, rawSecret);
        setLocalKeys(getStoredKeys());
        navigator.clipboard.writeText(rawSecret);
        notify.success('API key rotated! New key copied to clipboard.');
      }
      setRawKeyData(data);
      fetchKeys();
    } catch (err) {
      notify.error(err, 'Failed to rotate API key');
    } finally {
      setRollingId(null);
    }
  };

  const handleRevoke = async (id) => {
    if (!window.confirm('Are you sure you want to revoke this API key? This action is permanent and cannot be undone.')) return;
    try {
      await apiKeyApi.revokeKey(id, 'User manually revoked from portal');
      notify.success('API key revoked successfully');
      fetchKeys();
    } catch (err) {
      notify.error(err, 'Failed to revoke API key');
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
                {keys.map((k) => {
                  const prefixClean = k.keyPrefix?.replace(/^ash_live_/, '');
                  const storedRawKey = localKeys[k.id] || localKeys[k.keyPrefix] || localKeys[prefixClean];
                  const hasStoredKey = !!storedRawKey;
                  const isCopied = copiedId === k.id;
                  const isRolling = rollingId === k.id;

                  return (
                    <tr key={k.id} className="hover:bg-slate-850/50">
                      <td className="py-3 font-sans font-bold text-white">{k.name}</td>
                      <td className="py-3">
                        <div className="flex items-center space-x-2">
                          {hasStoredKey ? (
                            <span className="text-emerald-400 font-mono">
                              {storedRawKey.substring(0, 18)}••••••••
                            </span>
                          ) : (
                            <span className="text-indigo-300">
                              ash_live_{prefixClean}_••••••••••••
                            </span>
                          )}

                          {hasStoredKey ? (
                            <button
                              type="button"
                              onClick={() => handleCopyKey(storedRawKey, k.id)}
                              className="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition flex items-center space-x-1 text-[11px]"
                              title="Copy full active API key"
                            >
                              {isCopied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-indigo-400" />}
                              <span>{isCopied ? 'Copied' : 'Copy'}</span>
                            </button>
                          ) : (
                            k.status === 'ACTIVE' && (
                              <button
                                type="button"
                                disabled={isRolling}
                                onClick={() => handleRotateKey(k.id)}
                                className="px-2 py-0.5 rounded bg-indigo-600/20 hover:bg-indigo-600/30 border border-indigo-500/30 text-indigo-300 hover:text-white transition flex items-center space-x-1 text-[11px]"
                                title="Re-reveal / rotate key to get a fresh copy"
                              >
                                <RefreshCw className={`w-3 h-3 ${isRolling ? 'animate-spin' : ''}`} />
                                <span>{isRolling ? 'Rotating...' : 'Rotate & Copy'}</span>
                              </button>
                            )
                          )}
                        </div>
                      </td>
                      <td className="py-3">
                        <StatusBadge status={k.status} />
                      </td>
                      <td className="py-3 text-slate-400">{k.rateLimitRps || 10} RPS</td>
                      <td className="py-3 text-slate-500">
                        {new Date(k.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-3 text-right">
                        <div className="flex items-center justify-end space-x-1">
                          {hasStoredKey && (
                            <button
                              type="button"
                              onClick={() => handleCopyKey(storedRawKey, k.id)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-400 hover:bg-slate-800 transition"
                              title="Copy API Key"
                            >
                              <Copy className="w-4 h-4" />
                            </button>
                          )}
                          {k.status === 'ACTIVE' && (
                            <>
                              <button
                                type="button"
                                disabled={isRolling}
                                onClick={() => handleRotateKey(k.id)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-400 hover:bg-slate-800 transition"
                                title="Rotate / Re-roll Key"
                              >
                                <RefreshCw className={`w-4 h-4 ${isRolling ? 'animate-spin' : ''}`} />
                              </button>
                              <button
                                type="button"
                                onClick={() => handleRevoke(k.id)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
                                title="Revoke Key"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
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
