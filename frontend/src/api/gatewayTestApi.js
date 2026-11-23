import axios from 'axios';

// Interactive Gateway caller invoking /gateway/{apiSlug}/** with live metrics
export const gatewayTestApi = {
  invoke: async ({ apiSlug, path, method, apiKey, headers = {}, body = null }) => {
    const startTime = performance.now();
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    const url = `/gateway/${apiSlug}${cleanPath}`;

    const requestHeaders = {
      ...headers,
      'X-API-Key': apiKey,
    };

    try {
      const response = await axios({
        url,
        method,
        headers: requestHeaders,
        data: body,
        validateStatus: () => true, // Don't throw on 4xx/5xx so playground can inspect status & response
        timeout: 15000,
      });

      const durationMs = Math.round(performance.now() - startTime);

      return {
        status: response.status,
        statusText: response.statusText,
        durationMs,
        headers: response.headers,
        data: response.data,
      };
    } catch (err) {
      const durationMs = Math.round(performance.now() - startTime);
      return {
        status: err.response?.status || 500,
        statusText: err.response?.statusText || 'Network Error',
        durationMs,
        headers: err.response?.headers || {},
        data: err.response?.data || { error: err.message },
      };
    }
  },
};
