import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const ProtectedRoute = ({ allowedRoles = [] }) => {
  const { user, isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center">
        <div className="flex flex-col items-center space-y-3">
          <div className="w-10 h-10 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
          <span className="text-sm font-medium text-slate-400">Verifying session...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const userRole = user?.role || 'DEVELOPER';

  if (allowedRoles.length > 0 && !allowedRoles.includes(userRole)) {
    // Redirect to respective dashboard if role doesn't match
    if (userRole === 'OWNER') return <Navigate to="/owner/dashboard" replace />;
    if (userRole === 'ADMIN') return <Navigate to="/admin/dashboard" replace />;
    return <Navigate to="/dev/dashboard" replace />;
  }

  return <Outlet />;
};
