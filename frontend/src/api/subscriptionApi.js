import client from './client';

export const subscriptionApi = {
  subscribe: async (payload) => {
    const response = await client.post('/subscriptions', payload);
    return response.data;
  },

  getMySubscriptions: async (params = {}) => {
    const response = await client.get('/subscriptions/my', { params });
    return response.data;
  },

  getSubscription: async (id) => {
    const response = await client.get(`/subscriptions/${id}`);
    return response.data;
  },

  cancelSubscription: async (id) => {
    const response = await client.delete(`/subscriptions/${id}`);
    return response.data;
  },
};
