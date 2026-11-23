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
  Code
} from 'lucide-react';

export const ApiPlaygroundPage = () => {
  const [keys, setKeys] = useState([]);
  const [apis, setApis] = useState([]);
  const [loadingInitial, setLoadingInitial] = useState(true);

  // Form inputs
  const [selectedApiKey, setSelectedApiKey] = useState('');
  const [selectedApiSlug, setSelectedApiSlug] = useState('');
  const [method, setMethod] = useState('GET');
  const [subpath, setSubpath] = useState('/current');
  const [requestBody, setRequestBody] = useState('{\n  "query": "sample"\n}');
  
  // Execution state
  const [invoking, setInvoking] = useState(false);
  const [responseResult, setResponseResult] = useState(null);

  useEffect(() => {
    Promise.allSettled([
      apiKeyApi.getMyKeys({ page: 0, size: 50 }),
      catalogApi.listCatalog({ page: 0, size: 50 }),
    ]).then(([keyRes, apiRes]) => {
      if (keyRes.status === 'fulfilled') {
        const keyList = keyRes.value.content || [];
        setKeys(keyList);
        if (keyList.length > 0) {
          // Note: for testing in playground, developer can enter their raw key or select their key prefix
          setSelectedApiKey('');
        }
      }
      if (apiRes.status === 'fulfilled') {
        const apiList = apiRes.value.content || [];
        setApis(apiList);
        if (apiList.length > 0) {
          setSelectedApiSlug(apiList[0].slug);
        }
      }
      setLoadingInitial(false);
    });
  }, []);

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
      apiKey: selectedApiKey,
      body: parsedBody,
    });

    setResponseResult(result);
    setInvoking(false);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Header */}
      <div className="pb-6 border-b border-slate-800">
        <h1 className="text-3xl font-extrabold text-white tracking-tight flex items-center space-x-3">
          <Terminal className="w-8 h-8 text-emerald-400" />
          <span>Interactive API Playground</span>
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          Execute live proxy requests through the Spring Cloud Gateway (<code className="text-indigo-400">/gateway/{'{slug}'}/**</code>) with token bucket rate limiting and telemetry inspection.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left: Request Builder Form */}
        <div className="lg:col-span-5 p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-5">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-300">
            Request Configuration
          </h2>

          <form onSubmit={handleExecute} className="space-y-4">
            {/* Target API Selection */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">
                Target API
              </label>
              <select
                value={selectedApiSlug}
                onChange={(e) => setSelectedApiSlug(e.target.value)}
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                {apis.map((api) => (
                  <option key={api.slug} value={api.slug}>
                    {api.name} (/gateway/{api.slug}/**)
                  </option>
                ))}
              </select>
            </div>

            {/* API Key */}
            <div>
              <label className="block text-xs font-semibold text-slate-400 mb-1">
                API Key (Header: <code className="text-indigo-300">X-API-Key</code>)
              </label>
              <input
                type="text"
                required
                value={selectedApiKey}
                onChange={(e) => setSelectedApiKey(e.target.value)}
                placeholder="ash_live_xxxxxx_xxxxxxxxxxxxxx"
                className="w-full px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-emerald-400 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
              />
              <p className="text-[10px] text-slate-500 mt-1">
                Enter your raw key from the API Keys tab.
              </p>
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
                  placeholder="/current"
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
                  Configure your endpoint path, enter your <code className="text-indigo-400">ash_live_...</code> key, and click execute.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
