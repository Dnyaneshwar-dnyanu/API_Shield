import client from './client';

export const notificationApi = {
  getMyNotifications: async (params = {}) => {
    const response = await client.get('/notifications/my', { params });
    return response.data;
  },

  markAsRead: async (id) => {
    const response = await client.put(`/notifications/${id}/read`);
    return response.data;
  },

  getUnreadCount: async () => {
    const response = await client.get('/notifications/unread-count');
    return response.data;
  },
};
