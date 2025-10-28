import React, { createContext, useContext, useEffect, useState } from 'react';
import api from '../api/apiClient';
import type { AuthLoginCommand, AuthRegisterCommand, AuthResponseDto, UserDto, ProgressSubmitResponseDto } from '../types/api';

type AuthContextType = {
  user?: UserDto;
  isAuthenticated: boolean;
  loading: boolean;
  login: (creds: AuthLoginCommand) => Promise<void>;
  register: (creds: AuthRegisterCommand) => Promise<void>;
  logout: () => void;
  refresh: () => Promise<void>;
  refreshUser: () => Promise<void>;
  updateUserFromProgress: (resp: ProgressSubmitResponseDto) => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserDto | undefined>(undefined);
  // start as 'true' so we wait for initAuth on first render and avoid immediate redirect
  const [loading, setLoading] = useState(true);

  const isAuthenticated = Boolean(localStorage.getItem('accessToken')) && Boolean(user);

  const fetchUserRaw = async () => {
    // debug
    // eslint-disable-next-line no-console
    console.debug('AuthContext.fetchUserRaw: starting GET /users/me');
    const res = await api.get<UserDto>('/users/me');
    // eslint-disable-next-line no-console
    console.debug('AuthContext.fetchUserRaw: success', res?.data);
    setUser(res.data);
    if (res.data?.id) localStorage.setItem('userId', res.data.id);
  };

  // Public helper to allow other components to refresh the user data
  const refreshUser = async () => {
    try {
      await fetchUserRaw();
    } catch (e) {
      // pass through error handling already present in fetchUser
      await fetchUser();
    }
  };

  const fetchUser = async () => {
    setLoading(true);
    try {
      await fetchUserRaw();
    } catch (e: any) {
      // if fetching user fails, clear tokens to avoid inconsistent state
      // eslint-disable-next-line no-console
      console.error('fetchUser error', e?.response?.status ?? e?.message ?? e);
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userId');
      setUser(undefined);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // On app start: if accessToken exists, fetch user.
    // If accessToken is missing but refreshToken exists, attempt refresh and then fetch user.
    const initAuth = async () => {
      setLoading(true);
      try {
        const accessToken = localStorage.getItem('accessToken');
        // eslint-disable-next-line no-console
        console.debug('AuthContext.initAuth: accessToken present?', !!accessToken);
        if (accessToken) {
          try {
            // ensure axios default header is set so fetchUserRaw uses it
            api.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
            await fetchUserRaw();
            return;
          } catch (err: any) {
            // eslint-disable-next-line no-console
            console.warn('fetchUserRaw failed with accessToken, will try refresh if possible', err?.response?.status ?? err?.message ?? err);
            // proceed to try refresh below
          }
        }

        const refreshToken = localStorage.getItem('refreshToken');
        // eslint-disable-next-line no-console
        console.debug('AuthContext.initAuth: refreshToken present?', !!refreshToken);
        if (refreshToken) {
          try {
            const resp = await api.post<AuthResponseDto>('/auth/refresh', { refreshToken });
            // eslint-disable-next-line no-console
            console.debug('AuthContext.initAuth: refresh response', resp?.data);
            const data = resp.data;
            if (data?.accessToken) {
              if (data.accessToken) localStorage.setItem('accessToken', data.accessToken);
              if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
              api.defaults.headers.common.Authorization = `Bearer ${data.accessToken}`;
              // now fetch the user
              await fetchUserRaw();
              return;
            }
          } catch (e: any) {
            // eslint-disable-next-line no-console
            console.error('refresh token on init failed', e?.response?.status ?? e?.message ?? e);
            // fallthrough to clearing tokens below
          }
        }

        // If we reach here, we couldn't fetch the user; clear tokens/state
        // eslint-disable-next-line no-console
        console.debug('AuthContext.initAuth: unable to obtain user, clearing tokens and leaving unauthenticated');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userId');
        setUser(undefined);
      } finally {
        setLoading(false);
      }
    };

    void initAuth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setTokens = (auth: AuthResponseDto) => {
    if (auth.accessToken) {
      localStorage.setItem('accessToken', auth.accessToken);
      api.defaults.headers.common.Authorization = `Bearer ${auth.accessToken}`;
    }
    if (auth.refreshToken) localStorage.setItem('refreshToken', auth.refreshToken);
  };

  const login = async (creds: AuthLoginCommand) => {
    setLoading(true);
    try {
      const resp = await api.post<AuthResponseDto>('/auth/login', creds);
      setTokens(resp.data);
      await fetchUser();
    } finally {
      setLoading(false);
    }
  };

  const register = async (creds: AuthRegisterCommand) => {
    setLoading(true);
    try {
      const resp = await api.post<AuthResponseDto>('/auth/register', creds);
      setTokens(resp.data);
      await fetchUser();
    } finally {
      setLoading(false);
    }
  };

  const refresh = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) throw new Error('No refresh token');
    const resp = await api.post<AuthResponseDto>('/auth/refresh', { refreshToken });
    setTokens(resp.data);
    return;
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    setUser(undefined);
    // optional: redirect
    try {
      if (typeof window !== 'undefined') window.location.href = '/login';
    } catch {}
  };

  const updateUserFromProgress = (resp: ProgressSubmitResponseDto) => {
    // debug: log when user context is updated from progress
    // eslint-disable-next-line no-console
    console.debug('AuthContext.updateUserFromProgress called with:', resp);
    setUser((prev) => {
      if (!prev) return prev;
      const updated: UserDto = {
        ...prev,
        points: resp.userPoints,
        currentLevel: resp.newLevel !== undefined && resp.newLevel !== null ? resp.newLevel : prev.currentLevel,
        // update stars by adding awarded stars if prev.stars exists
        stars: (typeof prev.stars === 'number' ? prev.stars + (resp.starsAwarded ?? 0) : (resp.starsAwarded ?? 0)),
      };
      return updated;
    });
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, loading, login, register, logout, refresh, refreshUser, updateUserFromProgress }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
