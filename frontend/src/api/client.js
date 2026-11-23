import axios from 'axios';

// Base Axios instance pointing to Edge API Gateway
const client = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Request interceptor: attach JWT Bearer and Correlation ID
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('apishield_access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Generate lightweight UUIDv4 for correlation tracing
    if (!config.headers['X-Correlation-ID']) {
      config.headers['X-Correlation-ID'] = 'ui-' + Math.random().toString(36).substring(2, 15);
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 with automatic token refresh
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('apishield_refresh_token');

      if (!refreshToken || originalRequest.url.includes('/auth/refresh') || originalRequest.url.includes('/auth/login')) {
        // Clear session and notify listeners
        localStorage.removeItem('apishield_access_token');
        localStorage.removeItem('apishield_refresh_token');
        localStorage.removeItem('apishield_user');
        window.dispatchEvent(new Event('auth:unauthorized'));
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return client(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post('/api/v1/auth/refresh', {
          refreshToken,
        });

        const newAccessToken = data.accessToken;
        localStorage.setItem('apishield_access_token', newAccessToken);
        if (data.refreshToken) {
          localStorage.setItem('apishield_refresh_token', data.refreshToken);
        }

        client.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        processQueue(null, newAccessToken);
        return client(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        localStorage.removeItem('apishield_access_token');
        localStorage.removeItem('apishield_refresh_token');
        localStorage.removeItem('apishield_user');
        window.dispatchEvent(new Event('auth:unauthorized'));
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default client;
