import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { notificationApi } from '../api/notificationApi';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const data = await notificationApi.getUnreadCount();
      setUnreadCount(data.unreadCount || 0);
    } catch {
      // Ignore background error
    }
  }, [isAuthenticated]);

  const fetchNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    setLoading(true);
    try {
      const data = await notificationApi.getMyNotifications({ page: 0, size: 20 });
      setNotifications(data.content || []);
    } catch {
      // Ignore error
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      fetchUnreadCount();
      const interval = setInterval(fetchUnreadCount, 30000); // 30s poll
      return () => clearInterval(interval);
    } else {
      setUnreadCount(0);
      setNotifications([]);
    }
  }, [isAuthenticated, fetchUnreadCount]);

  const toggleDrawer = () => {
    if (!isDrawerOpen) {
      fetchNotifications();
    }
    setIsDrawerOpen((prev) => !prev);
  };

  const markAsRead = async (id) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // Ignore error
    }
  };

  return (
    <NotificationContext.Provider
      value={{
        unreadCount,
        notifications,
        isDrawerOpen,
        loading,
        toggleDrawer,
        markAsRead,
        refresh: fetchNotifications,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider');
  }
  return context;
};
