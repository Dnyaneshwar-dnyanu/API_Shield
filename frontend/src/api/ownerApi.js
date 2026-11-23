import client from './client';

export const ownerApi = {
  getMyApis: async (params = {}) => {
    const response = await client.get('/apis', { params });
    return response.data;
  },

  getApi: async (id) => {
    const response = await client.get(`/apis/${id}`);
    return response.data;
  },

  createApi: async (payload) => {
    const response = await client.post('/apis', payload);
    return response.data;
  },

  updateApi: async (id, payload) => {
    const response = await client.put(`/apis/${id}`, payload);
    return response.data;
  },

  publishApi: async (id) => {
    const response = await client.post(`/apis/${id}/publish`);
    return response.data;
  },

  unpublishApi: async (id) => {
    const response = await client.post(`/apis/${id}/unpublish`);
    return response.data;
  },

  addEndpoint: async (apiId, payload) => {
    const response = await client.post(`/apis/${apiId}/endpoints`, payload);
    return response.data;
  },

  addPlan: async (apiId, payload) => {
    const response = await client.post(`/apis/${apiId}/plans`, payload);
    return response.data;
  },
};
