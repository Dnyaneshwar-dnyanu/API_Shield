import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem('apishield_user');
      return savedUser ? JSON.parse(savedUser) : null;
    } catch {
      return null;
    }
  });

  const [accessToken, setAccessToken] = useState(() => {
    return localStorage.getItem('apishield_access_token') || null;
  });

  const [loading, setLoading] = useState(true);

  // Sync session on mount
  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
      setAccessToken(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);

    if (accessToken && !user) {
      authApi.getProfile()
        .then((profile) => {
          setUser(profile);
          localStorage.setItem('apishield_user', JSON.stringify(profile));
        })
        .catch(() => {
          setUser(null);
          setAccessToken(null);
          localStorage.removeItem('apishield_access_token');
          localStorage.removeItem('apishield_refresh_token');
          localStorage.removeItem('apishield_user');
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }

    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [accessToken, user]);

  const login = async (email, password) => {
    const data = await authApi.login({ email, password });
    localStorage.setItem('apishield_access_token', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('apishield_refresh_token', data.refreshToken);
    }
    setAccessToken(data.accessToken);

    const userObj = {
      id: data.userId,
      email: data.email,
      name: data.name || email.split('@')[0],
      role: data.role,
    };
    setUser(userObj);
    localStorage.setItem('apishield_user', JSON.stringify(userObj));
    return userObj;
  };

  const register = async (payload) => {
    return await authApi.register(payload);
  };

  const logout = () => {
    localStorage.removeItem('apishield_access_token');
    localStorage.removeItem('apishield_refresh_token');
    localStorage.removeItem('apishield_user');
    setUser(null);
    setAccessToken(null);
  };

  const role = user?.role || null;
  const isOwner = role === 'OWNER';
  const isDeveloper = role === 'DEVELOPER';
  const isAdmin = role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken,
        role,
        isOwner,
        isDeveloper,
        isAdmin,
        isAuthenticated: !!user,
        loading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
