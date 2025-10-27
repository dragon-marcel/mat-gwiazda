import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// If an accessToken already exists in localStorage when the module is imported,
// ensure axios default header is set so early requests include it.
const initialToken = localStorage.getItem('accessToken');
if (initialToken) {
  api.defaults.headers.common.Authorization = `Bearer ${initialToken}`;
  // eslint-disable-next-line no-console
  console.debug('apiClient: initial Authorization header set from localStorage');
}

// Request interceptor: attach access token if present and X-User-Id automatically
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const userId = localStorage.getItem('userId');
  if (userId && config.headers) {
    // attach X-User-Id for endpoints that require it
    config.headers['X-User-Id'] = userId;
  }
  // debug: show outgoing request url and whether token is attached
  // eslint-disable-next-line no-console
  console.debug('api.request', { url: config.url, hasToken: !!token, userId: userId ?? null });
  return config;
});

// Response interceptor: try refresh on 401 once
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config;
    if (!originalRequest) return Promise.reject(error);

    const status = error.response?.status;
    const isRetry = originalRequest._retry;

    // debug: log 401 occurrence
    // eslint-disable-next-line no-console
    console.debug('api.response.error', { url: originalRequest.url, status, isRetry });

    if (status === 401 && !isRetry) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');
      // eslint-disable-next-line no-console
      console.debug('api.response.error: attempting refresh?', !!refreshToken);
      if (refreshToken) {
        try {
          const resp = await api.post('/auth/refresh', { refreshToken });
          // eslint-disable-next-line no-console
          console.debug('api.response.error: refresh response', resp?.status, resp?.data);
          const { accessToken, refreshToken: newRefresh } = resp.data || {};
          if (accessToken) {
            localStorage.setItem('accessToken', accessToken);
            if (newRefresh) localStorage.setItem('refreshToken', newRefresh);
            api.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
            if (originalRequest.headers) originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return api(originalRequest);
          }
        } catch (e) {
          // Refresh failed: clear tokens and propagate error. Do NOT redirect here — allow AuthContext to handle it.
          // eslint-disable-next-line no-console
          console.error('api.response.error: refresh failed', e?.response?.status ?? e?.message ?? e);
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          return Promise.reject(e);
        }
      }
    }

    return Promise.reject(error);
  }
);

export default api;
