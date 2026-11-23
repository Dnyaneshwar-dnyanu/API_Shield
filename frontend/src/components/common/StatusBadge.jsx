import React from 'react';

export const StatusBadge = ({ status, className = '' }) => {
  const normalized = (status || '').toUpperCase();

  const getStyle = () => {
    switch (normalized) {
      case 'ACTIVE':
      case 'PUBLISHED':
      case 'SENT':
      case 'SUCCESS':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'PENDING':
      case 'DRAFT':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'CANCELLED':
      case 'SUSPENDED':
      case 'REVOKED':
      case 'FAILED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/30';
      case 'FREE':
        return 'bg-sky-500/10 text-sky-400 border-sky-500/30';
      case 'PRO':
        return 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30';
      case 'ENTERPRISE':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/30';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getStyle()} ${className}`}
    >
      <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-current opacity-80" />
      {normalized}
    </span>
  );
};
