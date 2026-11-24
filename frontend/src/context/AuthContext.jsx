import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem('apishield_user');
      if (!savedUser) return null;
      const parsed = JSON.parse(savedUser);
      // Auto-heal missing role from earlier sessions
      if (parsed && !parsed.role) {
        parsed.role = 'DEVELOPER';
      }
      return parsed;
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

    if (accessToken && (!user || !user.role)) {
      authApi.getProfile()
        .then((profile) => {
          const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ');
          const userObj = {
            id: profile.id,
            email: profile.email,
            firstName: profile.firstName,
            lastName: profile.lastName,
            name: fullName || profile.email?.split('@')[0],
            role: profile.role || 'DEVELOPER',
          };
          setUser(userObj);
          localStorage.setItem('apishield_user', JSON.stringify(userObj));
        })
        .catch(() => {
          if (user && !user.role) {
            const updated = { ...user, role: 'DEVELOPER' };
            setUser(updated);
            localStorage.setItem('apishield_user', JSON.stringify(updated));
          }
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

    const u = data.user || {};
    const fullName = [u.firstName, u.lastName].filter(Boolean).join(' ');
    const userObj = {
      id: u.id || data.userId,
      email: u.email || data.email || email,
      firstName: u.firstName,
      lastName: u.lastName,
      name: fullName || data.name || email.split('@')[0],
      role: u.role || data.role || 'DEVELOPER',
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

  const role = user?.role || (user ? 'DEVELOPER' : null);
  const isOwner = role === 'OWNER' || role === 'API_OWNER';
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
