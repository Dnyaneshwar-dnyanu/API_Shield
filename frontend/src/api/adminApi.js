import client from './client';

export const adminApi = {
  getUsers: async (params = {}) => {
    const response = await client.get('/admin/users', { params });
    return response.data;
  },

  suspendUser: async (id, reason) => {
    const response = await client.put(`/admin/users/${id}/suspend`, { reason });
    return response.data;
  },

  reactivateUser: async (id) => {
    const response = await client.put(`/admin/users/${id}/reactivate`);
    return response.data;
  },
};
