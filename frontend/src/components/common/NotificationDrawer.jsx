import React from 'react';
import { useNotification } from '../../context/NotificationContext';
import { X, Check, Mail, Bell, ShieldAlert, CreditCard } from 'lucide-react';

export const NotificationDrawer = () => {
  const { isDrawerOpen, toggleDrawer, notifications, loading, markAsRead } = useNotification();

  if (!isDrawerOpen) return null;

  const getIcon = (templateKey) => {
    switch (templateKey) {
      case 'WELCOME_EMAIL':
        return <Mail className="w-4 h-4 text-emerald-400" />;
      case 'KEY_REVOKED':
        return <ShieldAlert className="w-4 h-4 text-rose-400" />;
      case 'PAYMENT_SUCCESS':
      case 'SUBSCRIPTION_ACTIVATED':
        return <CreditCard className="w-4 h-4 text-indigo-400" />;
      default:
        return <Bell className="w-4 h-4 text-sky-400" />;
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div 
        className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm transition-opacity"
        onClick={toggleDrawer}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-slate-900 border-l border-slate-800 p-6 flex flex-col shadow-2xl">
          {/* Header */}
          <div className="flex items-center justify-between pb-4 border-b border-slate-800">
            <div className="flex items-center space-x-2">
              <Bell className="w-5 h-5 text-indigo-400" />
              <h2 className="text-lg font-bold text-white">Notifications</h2>
            </div>
            <button
              onClick={toggleDrawer}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto py-4 space-y-3">
            {loading ? (
              <div className="flex justify-center py-10">
                <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="text-center py-16 text-slate-500">
                <Bell className="w-10 h-10 mx-auto mb-2 opacity-30" />
                <p className="text-sm">No notifications yet</p>
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  className={`p-3.5 rounded-xl border transition ${
                    item.isRead
                      ? 'bg-slate-900/60 border-slate-800/80 text-slate-400'
                      : 'bg-slate-850 border-indigo-500/30 text-slate-100 shadow-md'
                  }`}
                >
                  <div className="flex items-start justify-between space-x-2">
                    <div className="flex items-center space-x-2">
                      <div className="p-1.5 rounded-lg bg-slate-800">
                        {getIcon(item.templateKey)}
                      </div>
                      <span className="font-semibold text-sm text-white">{item.title}</span>
                    </div>

                    {!item.isRead && (
                      <button
                        onClick={() => markAsRead(item.id)}
                        className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center space-x-1"
                        title="Mark as read"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Read</span>
                      </button>
                    )}
                  </div>

                  <p className="text-xs text-slate-400 mt-2 line-clamp-2">
                    {item.contentPayload}
                  </p>

                  <div className="flex items-center justify-between mt-3 text-[10px] text-slate-500 font-mono">
                    <span>{item.channel}</span>
                    <span>{new Date(item.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
