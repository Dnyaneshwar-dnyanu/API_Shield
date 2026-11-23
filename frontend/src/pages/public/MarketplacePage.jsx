import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { catalogApi } from '../../api/catalogApi';
import { Search, Filter, Layers, ArrowRight, Zap, Check } from 'lucide-react';
import { StatusBadge } from '../../components/common/StatusBadge';

const CATEGORIES = [
  'All',
  'AI & ML',
  'Finance & Crypto',
  'Weather & Data',
  'E-Commerce',
  'Security & Auth',
  'Developer Tools',
];

export const MarketplacePage = () => {
  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchCatalog = () => {
    setLoading(true);
    const categoryParam = selectedCategory === 'All' ? undefined : selectedCategory;
    catalogApi.listCatalog({
      search: searchQuery || undefined,
      category: categoryParam,
      page,
      size: 12,
    })
      .then((data) => {
        setApis(data.content || []);
        setTotalPages(data.totalPages || 1);
      })
      .catch(() => setApis([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchCatalog();
  }, [selectedCategory, page]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchCatalog();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between pb-8 border-b border-slate-800">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">API Marketplace</h1>
          <p className="text-slate-400 text-sm mt-1 max-w-xl">
            Discover, subscribe to, and consume high-performance APIs protected by APIShield Edge Gateway.
          </p>
        </div>

        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} className="mt-4 md:mt-0 relative w-full md:w-80">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search APIs or endpoints..."
            className="w-full pl-10 pr-4 py-2 rounded-xl bg-slate-900 border border-slate-800 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition"
          />
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
        </form>
      </div>

      {/* Category Pills */}
      <div className="flex items-center space-x-2 overflow-x-auto py-6 scrollbar-none">
        <Filter className="w-4 h-4 text-slate-500 flex-shrink-0 mr-1" />
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            onClick={() => {
              setSelectedCategory(cat);
              setPage(0);
            }}
            className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              selectedCategory === cat
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                : 'bg-slate-900 text-slate-400 hover:text-white hover:bg-slate-800 border border-slate-800'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Catalog Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 py-12">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="h-64 rounded-2xl bg-slate-900/50 animate-pulse border border-slate-800/60" />
          ))}
        </div>
      ) : apis.length === 0 ? (
        <div className="text-center py-24 rounded-2xl bg-slate-900/40 border border-dashed border-slate-800 my-6">
          <Layers className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white">No APIs Found</h3>
          <p className="text-sm text-slate-400 max-w-sm mx-auto mt-1">
            Try adjusting your category filter or search keywords.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 py-4">
          {apis.map((api) => (
            <div
              key={api.id}
              className="p-6 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between hover:border-indigo-500/40 transition duration-200 group"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-semibold px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    {api.category || 'General'}
                  </span>
                  <StatusBadge status="PUBLISHED" />
                </div>

                <h3 className="text-lg font-bold text-white group-hover:text-indigo-400 transition">
                  {api.name}
                </h3>

                <div className="flex items-center space-x-1.5 text-xs font-mono text-slate-400 mt-1">
                  <span className="text-emerald-400">PROXY:</span>
                  <span>/gateway/{api.slug}/**</span>
                </div>

                <p className="text-sm text-slate-400 mt-3 line-clamp-3">
                  {api.description || 'Enterprise API ready for integration.'}
                </p>
              </div>

              <div className="pt-6 mt-6 border-t border-slate-800 flex items-center justify-between">
                <div className="text-xs text-slate-400">
                  <span className="font-semibold text-white">{api.plans?.length || 1}</span> Tier(s) Available
                </div>
                <Link
                  to={`/marketplace/${api.slug}`}
                  className="px-3.5 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600 text-indigo-300 hover:text-white text-xs font-semibold flex items-center space-x-1 transition"
                >
                  <span>View Details</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center space-x-3 pt-10">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 rounded-lg bg-slate-900 border border-slate-800 text-xs font-medium disabled:opacity-40"
          >
            Previous
          </button>
          <span className="text-xs font-mono text-slate-400">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 rounded-lg bg-slate-900 border border-slate-800 text-xs font-medium disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};
