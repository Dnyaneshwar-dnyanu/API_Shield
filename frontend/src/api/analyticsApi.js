import client from './client';

export const analyticsApi = {
  getOwnerOverview: async (params = {}) => {
    const response = await client.get('/analytics/owner/overview', { params });
    return response.data;
  },

  getApiTimeseries: async (apiId, days = 7) => {
    const response = await client.get(`/analytics/owner/apis/${apiId}/timeseries`, {
      params: { days },
    });
    return response.data;
  },

  getDeveloperQuota: async () => {
    const response = await client.get('/analytics/developer/quota');
    return response.data;
  },

  getDeveloperLogs: async (params = {}) => {
    const response = await client.get('/analytics/developer/logs', { params });
    return response.data;
  },
};
