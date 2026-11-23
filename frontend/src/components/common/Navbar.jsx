import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import { 
  Shield, 
  Bell, 
  LogOut, 
  User, 
  Menu, 
  X, 
  Terminal, 
  Key, 
  Layers, 
  BarChart2, 
  PlusCircle, 
  Store,
  Settings
} from 'lucide-react';

export const Navbar = () => {
  const { user, isAuthenticated, isOwner, isDeveloper, isAdmin, logout } = useAuth();
  const { unreadCount, toggleDrawer } = useNotification();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="sticky top-0 z-40 bg-slate-900/80 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo */}
          <div className="flex items-center space-x-3">
            <Link to="/" className="flex items-center space-x-2.5 group">
              <div className="w-9 h-9 rounded-lg bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center group-hover:bg-indigo-600/30 transition">
                <Shield className="w-5 h-5 text-indigo-400 group-hover:scale-110 transition duration-200" />
              </div>
              <div className="flex flex-col">
                <span className="text-lg font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-indigo-300 bg-clip-text text-transparent">
                  APIShield
                </span>
                <span className="text-[10px] uppercase tracking-widest text-slate-400 font-semibold -mt-1">
                  Enterprise Gateway
                </span>
              </div>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center space-x-1 ml-6">
              <Link
                to="/marketplace"
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                  isActive('/marketplace')
                    ? 'bg-slate-800 text-indigo-400'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <div className="flex items-center space-x-1.5">
                  <Store className="w-4 h-4" />
                  <span>Marketplace</span>
                </div>
              </Link>

              {/* Developer Links */}
              {isDeveloper && (
                <>
                  <Link
                    to="/dev/dashboard"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/dev/dashboard')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    Dashboard
                  </Link>
                  <Link
                    to="/dev/subscriptions"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/dev/subscriptions')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    Subscriptions
                  </Link>
                  <Link
                    to="/dev/keys"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/dev/keys')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    <div className="flex items-center space-x-1.5">
                      <Key className="w-4 h-4" />
                      <span>API Keys</span>
                    </div>
                  </Link>
                  <Link
                    to="/dev/playground"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/dev/playground')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    <div className="flex items-center space-x-1.5">
                      <Terminal className="w-4 h-4" />
                      <span>Playground</span>
                    </div>
                  </Link>
                  <Link
                    to="/dev/usage"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/dev/usage')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    Usage
                  </Link>
                </>
              )}

              {/* Owner Links */}
              {isOwner && (
                <>
                  <Link
                    to="/owner/dashboard"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/owner/dashboard')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    Dashboard
                  </Link>
                  <Link
                    to="/owner/apis"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/owner/apis')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    <div className="flex items-center space-x-1.5">
                      <Layers className="w-4 h-4" />
                      <span>My APIs</span>
                    </div>
                  </Link>
                  <Link
                    to="/owner/apis/new"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/owner/apis/new')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    <div className="flex items-center space-x-1.5">
                      <PlusCircle className="w-4 h-4 text-emerald-400" />
                      <span>Publish API</span>
                    </div>
                  </Link>
                  <Link
                    to="/owner/analytics"
                    className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                      isActive('/owner/analytics')
                        ? 'bg-slate-800 text-indigo-400'
                        : 'text-slate-300 hover:text-white hover:bg-slate-800/50'
                    }`}
                  >
                    <div className="flex items-center space-x-1.5">
                      <BarChart2 className="w-4 h-4" />
                      <span>Analytics</span>
                    </div>
                  </Link>
                </>
              )}

              {/* Admin Link */}
              {isAdmin && (
                <Link
                  to="/admin/dashboard"
                  className={`px-3 py-1.5 rounded-md text-sm font-medium transition ${
                    isActive('/admin/dashboard')
                      ? 'bg-slate-800 text-amber-400'
                      : 'text-amber-300 hover:bg-slate-800/50'
                  }`}
                >
                  <div className="flex items-center space-x-1.5">
                    <Settings className="w-4 h-4" />
                    <span>Admin Console</span>
                  </div>
                </Link>
              )}
            </div>
          </div>

          {/* Right Section */}
          <div className="hidden md:flex items-center space-x-4">
            {isAuthenticated ? (
              <div className="flex items-center space-x-3">
                {/* Notifications Bell */}
                <button
                  onClick={toggleDrawer}
                  className="relative p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition"
                  title="Notifications"
                >
                  <Bell className="w-4 h-4" />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </button>

                {/* User Role Tag */}
                <div className="flex items-center space-x-2 pl-2 border-l border-slate-800">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center font-bold text-xs text-white">
                    {user?.name ? user.name[0].toUpperCase() : 'U'}
                  </div>
                  <div className="flex flex-col text-left">
                    <span className="text-xs font-semibold text-slate-200">
                      {user?.name || user?.email}
                    </span>
                    <span className="text-[10px] font-mono text-indigo-400 uppercase">
                      {user?.role}
                    </span>
                  </div>
                </div>

                {/* Logout Button */}
                <button
                  onClick={handleLogout}
                  className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
                  title="Sign Out"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-3">
                <Link
                  to="/login"
                  className="px-3.5 py-1.5 rounded-lg text-sm font-medium text-slate-300 hover:text-white transition"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-1.5 rounded-lg text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30 transition"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>

          {/* Mobile menu trigger */}
          <div className="md:hidden flex items-center space-x-2">
            {isAuthenticated && (
              <button
                onClick={toggleDrawer}
                className="relative p-2 rounded-lg bg-slate-800 text-slate-300"
              >
                <Bell className="w-4 h-4" />
                {unreadCount > 0 && (
                  <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[9px] font-bold flex items-center justify-center">
                    {unreadCount}
                  </span>
                )}
              </button>
            )}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-slate-900 border-b border-slate-800 px-4 pt-2 pb-4 space-y-1">
          <Link
            to="/marketplace"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
          >
            Marketplace
          </Link>
          {isDeveloper && (
            <>
              <Link
                to="/dev/dashboard"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                Developer Dashboard
              </Link>
              <Link
                to="/dev/subscriptions"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                My Subscriptions
              </Link>
              <Link
                to="/dev/keys"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                API Keys
              </Link>
              <Link
                to="/dev/playground"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                Playground
              </Link>
            </>
          )}
          {isOwner && (
            <>
              <Link
                to="/owner/dashboard"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                Owner Dashboard
              </Link>
              <Link
                to="/owner/apis"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                My APIs
              </Link>
              <Link
                to="/owner/apis/new"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-emerald-400 hover:bg-slate-800"
              >
                + Publish New API
              </Link>
              <Link
                to="/owner/analytics"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                Analytics
              </Link>
            </>
          )}
          {isAdmin && (
            <Link
              to="/admin/dashboard"
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 rounded-md text-base font-medium text-amber-400 hover:bg-slate-800"
            >
              Admin Console
            </Link>
          )}
          {isAuthenticated ? (
            <button
              onClick={() => {
                setMobileMenuOpen(false);
                handleLogout();
              }}
              className="w-full text-left px-3 py-2 rounded-md text-base font-medium text-rose-400 hover:bg-slate-800"
            >
              Sign Out
            </button>
          ) : (
            <div className="pt-2 border-t border-slate-800 space-y-1">
              <Link
                to="/login"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-slate-300 hover:bg-slate-800"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 rounded-md text-base font-medium text-indigo-400 hover:bg-slate-800"
              >
                Get Started
              </Link>
            </div>
          )}
        </div>
      )}
    </nav>
  );
};
