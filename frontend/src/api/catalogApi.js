import client from './client';

export const catalogApi = {
  listCatalog: async (params = {}) => {
    const response = await client.get('/catalog', { params });
    return response.data;
  },

  getCatalogDetail: async (slug) => {
    const response = await client.get(`/catalog/${slug}`);
    return response.data;
  },
};
