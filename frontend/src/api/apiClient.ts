import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: attach access token if present
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
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

    if (status === 401 && !isRetry) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          // Use the same api instance (baseURL already set to http://localhost:8080/api/v1)
          const resp = await api.post('/auth/refresh', { refreshToken });
          const { accessToken, refreshToken: newRefresh } = resp.data || {};
          if (accessToken) {
            localStorage.setItem('accessToken', accessToken);
            if (newRefresh) localStorage.setItem('refreshToken', newRefresh);
            api.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
            if (originalRequest.headers) originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return api(originalRequest);
          }
        } catch (e) {
          // refresh failed -> clear tokens and redirect to login
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          try {
            if (typeof window !== 'undefined') window.location.href = '/login';
          } catch {}
          return Promise.reject(e);
        }
      }
    }

    return Promise.reject(error);
  }
);

export default api;
