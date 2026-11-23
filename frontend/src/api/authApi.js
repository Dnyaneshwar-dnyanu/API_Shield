import client from './client';

export const authApi = {
  login: async (credentials) => {
    const response = await client.post('/auth/login', credentials);
    return response.data;
  },

  register: async (userData) => {
    const response = await client.post('/auth/register', userData);
    return response.data;
  },

  getProfile: async () => {
    const response = await client.get('/users/me');
    return response.data;
  },

  verifyEmail: async (token) => {
    const response = await client.get(`/auth/verify-email?token=${token}`);
    return response.data;
  },

  forgotPassword: async (email) => {
    const response = await client.post('/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (token, newPassword) => {
    const response = await client.post('/auth/reset-password', { token, newPassword });
    return response.data;
  },
};
