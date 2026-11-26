import client from './client';

export const apiKeyApi = {
  generateKey: async (payload) => {
    const response = await client.post('/keys', payload);
    return response.data;
  },

  getMyKeys: async (params = {}) => {
    const response = await client.get('/keys/my', { params });
    return response.data;
  },

  revokeKey: async (id, reason = 'User requested revocation') => {
    const response = await client.delete(`/keys/${id}`, {
      data: { reason },
    });
    return response.data;
  },

  rollKey: async (id) => {
    const response = await client.post(`/keys/${id}/roll`);
    return response.data;
  },
};
