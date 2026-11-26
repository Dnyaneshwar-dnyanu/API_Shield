import React, { useState, useEffect } from 'react';
import { apiKeyApi } from '../../api/apiKeyApi';
import { catalogApi } from '../../api/catalogApi';
import { gatewayTestApi } from '../../api/gatewayTestApi';
import { 
  Terminal, 
  Send, 
  Check, 
  Clock, 
  Key, 
  Layers, 
  AlertCircle,
  Code,
  Copy,
  ExternalLink,
  Sparkles,
  ShieldCheck
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { notify } from '../../utils/toast';

const STORAGE_KEY = 'apishield_developer_keys';

function getStoredKeys() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

export const ApiPlaygroundPage = () => {
  const [keys, setKeys] = useState([]);
  const [apis, setApis] = useState([]);
  const [apiEndpoints, setApiEndpoints] = useState([]);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [storedSecrets, setStoredSecrets] = useState(getStoredKeys());

  // Form inputs
  const [selectedKeyId, setSelectedKeyId] = useState('');
  const [selectedApiKey, setSelectedApiKey] = useState('');
  const [selectedApiSlug, setSelectedApiSlug] = useState('');
  const [method, setMethod] = useState('GET');
  const [subpath, setSubpath] = useState('/posts');
  const [requestBody, setRequestBody] = useState('{\n  "query": "sample"\n}');
  
  // Execution state
  const [invoking, setInvoking] = useState(false);
  const [responseResult, setResponseResult] = useState(null);
  const [copiedCurl, setCopiedCurl] = useState(false);

  // 1. Initial Load of Keys & APIs
  useEffect(() => {
    const secrets = getStoredKeys();
    setStoredSecrets(secrets);

    Promise.allSettled([
      apiKeyApi.getMyKeys({ page: 0, size: 50 }),
      catalogApi.listCatalog({ page: 0, size: 50 }),
    ]).then(([keyRes, apiRes]) => {
      let activeKeys = [];
      if (keyRes.status === 'fulfilled') {
        const rawVal = keyRes.value;
        const keyList = Array.isArray(rawVal) ? rawVal : (rawVal?.content || []);
        activeKeys = keyList.filter((k) => k.status === 'ACTIVE');
        setKeys(keyList);
      }

      if (apiRes.status === 'fulfilled') {
        const apiList = apiRes.value.content || [];
        setApis(apiList);
        if (apiList.length > 0) {
          const firstSlug = apiList[0].slug;
          setSelectedApiSlug(firstSlug);
          loadApiEndpoints(firstSlug);
        }
      }

      // Auto-populate best matching key
      if (activeKeys.length > 0) {
        const firstKey = activeKeys[0];
        setSelectedKeyId(firstKey.id);
        const secret = secrets[firstKey.id] || secrets[firstKey.keyPrefix] || '';
        if (secret) {
          setSelectedApiKey(secret);
        } else {
          setSelectedApiKey(`ash_live_${firstKey.keyPrefix}_`);
        }
      }

      setLoadingInitial(false);
    });
  }, []);

  // 2. Fetch Endpoints when selected API changes
  const loadApiEndpoints = async (slug) => {
    try {
      const detail = await catalogApi.getCatalogDetail(slug);
      const eps = detail.endpoints || [];
      setApiEndpoints(eps);
      if (eps.length > 0) {
        setSubpath(eps[0].pathPattern || '/posts');
        setMethod(eps[0].httpMethod || 'GET');
      }
    } catch {
      setApiEndpoints([]);
    }
  };

  const handleApiChange = (slug) => {
    setSelectedApiSlug(slug);
    loadApiEndpoints(slug);

    // Try finding key that matches this API
    const targetApi = apis.find((a) => a.slug === slug);
    if (targetApi) {
      const matchingKey = keys.find((k) => k.apiId === targetApi.id && k.status === 'ACTIVE');
      if (matchingKey) {
        setSelectedKeyId(matchingKey.id);
        const secret = storedSecrets[matchingKey.id] || storedSecrets[matchingKey.keyPrefix] || '';
        if (secret) setSelectedApiKey(secret);
      }
    }
  };

  const handleKeySelect = (keyId) => {
    setSelectedKeyId(keyId);
    if (!keyId) {
      setSelectedApiKey('');
      return;
    }
    const found = keys.find((k) => k.id === keyId);
    if (found) {
      const secret = storedSecrets[found.id] || storedSecrets[found.keyPrefix] || '';
      if (secret) {
        setSelectedApiKey(secret);
      } else {
        setSelectedApiKey(`ash_live_${found.keyPrefix}_`);
      }
    }
  };

  const handleExecute = async (e) => {
    e.preventDefault();
    if (!selectedApiSlug) return;

    setInvoking(true);
    setResponseResult(null);

    let parsedBody = null;
    if (['POST', 'PUT', 'PATCH'].includes(method) && requestBody) {
      try {
        parsedBody = JSON.parse(requestBody);
      } catch {
        parsedBody = requestBody;
      }
    }

    const result = await gatewayTestApi.invoke({
      apiSlug: selectedApiSlug,
      path: subpath,
      method,
      apiKey: selectedApiKey.trim(),
      body: parsedBody,
    });

    setResponseResult(result);
    setInvoking(false);
  };

  const cleanSubpath = subpath.startsWith('/') ? subpath : `/${subpath}`;
  const fullGatewayUrl = `http://localhost:8080/gateway/${selectedApiSlug}${cleanSubpath}`;
  const curlCommand = `curl.exe -X ${method} "${fullGatewayUrl}" \\\n  -H "X-API-Key: ${selectedApiKey || 'YOUR_API_KEY'}" \\\n  -H "Accept: application/json"${['POST', 'PUT', 'PATCH'].includes(method) ? ` \\\n  -H "Content-Type: application/json" \\\n  -d '${requestBody.replace(/\n/g, ' ')}'` : ''}`;

  const handleCopyCurl = () => {
    navigator.clipboard.writeText(curlCommand);
    setCopiedCurl(true);
    notify.success('cURL command copied to clipboard!');
    setTimeout(() => setCopiedCurl(false), 2000);
  };

  const isKeySecretCached = selectedKeyId && (storedSecrets[selectedKeyId] || keys.find(k => k.id === selectedKeyId && storedSecrets[k.keyPrefix]));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Header */}
      <div className="pb-6 border-b border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight flex items-center space-x-3">
            <Terminal className="w-8 h-8 text-emerald-400" />
            <span>Interactive API Playground</span>
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Execute live proxy requests through the Spring Cloud Gateway (<code className="text-indigo-400">/gateway/{'{slug}'}/**</code>) with token bucket rate limiting and telemetry inspection.
          </p>
        </div>

        <Link
          to="/developer/keys"
          className="inline-flex items-center space-x-2 text-xs font-semibold text-indigo-400 hover:text-indigo-300 bg-slate-900 border border-slate-800 px-3.5 py-2 rounded-xl transition self-start md:self-auto"
        >
          <Key className="w-3.5 h-3.5" />
          <span>Manage / Rotate Keys</span>
          <ExternalLink className="w-3 h-3 text-slate-500" />
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left: Request Builder Form */}
        <div className="lg:col-span-5 p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-5">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-300 flex items-center space-x-2">
            <Layers className="w-4 h-4 text-indigo-400" />
            <span>Request Configuration</span>
          </h2>

          <form onSubmit={handleExecute} className="space-y-4">
            {/* Target API Selection */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">
                Target API
              </label>
              <select
                value={selectedApiSlug}
                onChange={(e) => handleApiChange(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                {apis.map((api) => (
                  <option key={api.slug} value={api.slug}>
                    {api.name} (/gateway/{api.slug}/**)
                  </option>
                ))}
              </select>
            </div>

            {/* Quick Endpoint Selection */}
            {apiEndpoints.length > 0 && (
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1.5">
                  Published Endpoints
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {apiEndpoints.map((ep, idx) => {
                    const isSelected = subpath === ep.pathPattern && method === ep.httpMethod;
                    return (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => {
                          setSubpath(ep.pathPattern);
                          setMethod(ep.httpMethod || 'GET');
                        }}
                        className={`px-2.5 py-1 rounded-lg text-xs font-mono font-medium transition flex items-center space-x-1.5 ${
                          isSelected
                            ? 'bg-indigo-600 text-white shadow-sm'
                            : 'bg-slate-950 hover:bg-slate-850 text-slate-300 border border-slate-800'
                        }`}
                      >
                        <span className={`text-[10px] font-bold ${
                          ep.httpMethod === 'GET' ? 'text-emerald-400' :
                          ep.httpMethod === 'POST' ? 'text-sky-400' :
                          ep.httpMethod === 'DELETE' ? 'text-rose-400' : 'text-amber-400'
                        }`}>
                          {ep.httpMethod}
                        </span>
                        <span>{ep.pathPattern}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* API Key Selector & Input */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-semibold text-slate-400">
                  Select Active API Key
                </label>
                {isKeySecretCached && (
                  <span className="text-[10px] text-emerald-400 flex items-center space-x-1 font-mono">
                    <ShieldCheck className="w-3 h-3 text-emerald-400" />
                    <span>Secret Cached</span>
                  </span>
                )}
              </div>

              <select
                value={selectedKeyId}
                onChange={(e) => handleKeySelect(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                <option value="">-- Manual / Custom Key --</option>
                {keys.map((k) => {
                  const hasSecret = storedSecrets[k.id] || storedSecrets[k.keyPrefix];
                  return (
                    <option key={k.id} value={k.id}>
                      {k.name} ({k.keyPrefix}) {hasSecret ? '✓ saved secret' : ''} [{k.status}]
                    </option>
                  );
                })}
              </select>

              <div>
                <input
                  type="text"
                  required
                  value={selectedApiKey}
                  onChange={(e) => setSelectedApiKey(e.target.value)}
                  placeholder="ash_live_xxxxxx_xxxxxxxxxxxxxx"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-emerald-400 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
                />
                <p className="text-[10px] text-slate-500 mt-1 flex items-center justify-between">
                  <span>Header: <code className="text-indigo-300">X-API-Key</code></span>
                  {!isKeySecretCached && selectedKeyId && (
                    <Link to="/developer/keys" className="text-indigo-400 hover:underline">
                      Need secret? Rotate in Keys tab
                    </Link>
                  )}
                </p>
              </div>
            </div>

            {/* Method & Subpath */}
            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">
                  Method
                </label>
                <select
                  value={method}
                  onChange={(e) => setMethod(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono font-bold text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="DELETE">DELETE</option>
                </select>
              </div>

              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-400 mb-1">
                  Path
                </label>
                <input
                  type="text"
                  value={subpath}
                  onChange={(e) => setSubpath(e.target.value)}
                  placeholder="/posts"
                  className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            {/* Request Body (for POST/PUT) */}
            {['POST', 'PUT', 'PATCH'].includes(method) && (
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">
                  JSON Body
                </label>
                <textarea
                  rows={4}
                  value={requestBody}
                  onChange={(e) => setRequestBody(e.target.value)}
                  className="w-full p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300 focus:outline-none focus:border-indigo-500"
                />
              </div>
            )}

            <button
              type="submit"
              disabled={invoking || !selectedApiSlug}
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs shadow-lg shadow-indigo-600/30 flex items-center justify-center space-x-2 transition disabled:opacity-50"
            >
              {invoking ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>Execute Gateway Call</span>
                </>
              )}
            </button>
          </form>

          {/* cURL Command Generator */}
          <div className="pt-3 border-t border-slate-800/80">
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider flex items-center space-x-1">
                <Code className="w-3.5 h-3.5 text-indigo-400" />
                <span>Equivalent cURL</span>
              </span>
              <button
                type="button"
                onClick={handleCopyCurl}
                className="text-[11px] text-slate-400 hover:text-white flex items-center space-x-1 px-2 py-0.5 rounded bg-slate-950 hover:bg-slate-850 border border-slate-800 transition"
              >
                {copiedCurl ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3 text-slate-400" />}
                <span>{copiedCurl ? 'Copied' : 'Copy'}</span>
              </button>
            </div>
            <pre className="p-2.5 rounded-lg bg-slate-950 border border-slate-850 text-[11px] font-mono text-slate-400 overflow-x-auto whitespace-pre">
              {curlCommand}
            </pre>
          </div>
        </div>

        {/* Right: Live Response Inspector */}
        <div className="lg:col-span-7 p-6 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-4">
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-300 flex items-center space-x-2">
                <Code className="w-4 h-4 text-indigo-400" />
                <span>Gateway Response Inspector</span>
              </h2>

              {responseResult && (
                <div className="flex items-center space-x-3 text-xs font-mono">
                  <span className={`px-2.5 py-0.5 rounded font-bold ${
                    responseResult.status >= 200 && responseResult.status < 300
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                      : responseResult.status === 429
                      ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                      : 'bg-rose-500/10 text-rose-400 border border-rose-500/30'
                  }`}>
                    {responseResult.status} {responseResult.statusText}
                  </span>
                  <span className="text-slate-400 flex items-center space-x-1">
                    <Clock className="w-3.5 h-3.5 text-slate-500" />
                    <span>{responseResult.durationMs}ms</span>
                  </span>
                </div>
              )}
            </div>

            {responseResult ? (
              <div className="space-y-4">
                <div>
                  <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider block mb-1">
                    Response Payload
                  </span>
                  <pre className="p-4 rounded-xl bg-slate-950 border border-slate-850 text-xs font-mono text-emerald-400 overflow-x-auto max-h-96">
                    {typeof responseResult.data === 'object'
                      ? JSON.stringify(responseResult.data, null, 2)
                      : String(responseResult.data)}
                  </pre>
                </div>

                <div>
                  <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider block mb-1">
                    Edge Response Headers
                  </span>
                  <div className="p-3 rounded-xl bg-slate-950 border border-slate-850 text-[11px] font-mono text-slate-300 space-y-1 max-h-36 overflow-y-auto">
                    {Object.entries(responseResult.headers || {}).map(([k, v]) => (
                      <div key={k} className="flex">
                        <span className="text-slate-500 w-44 truncate">{k}:</span>
                        <span className="text-slate-200">{String(v)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-24 text-center text-slate-500">
                <Terminal className="w-12 h-12 mx-auto mb-3 opacity-30" />
                <p className="text-sm font-medium">Ready to execute requests</p>
                <p className="text-xs text-slate-600 mt-1 max-w-sm mx-auto">
                  Select your active API key or paste it, pick an endpoint, and click execute to route live traffic through the gateway.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
