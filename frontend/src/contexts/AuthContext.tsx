import React, { createContext, useContext, useEffect, useState } from 'react';
import api from '../api/apiClient';
import type { AuthLoginCommand, AuthRegisterCommand, AuthResponseDto, UserDto } from '../types/api';

type AuthContextType = {
  user?: UserDto;
  isAuthenticated: boolean;
  loading: boolean;
  login: (creds: AuthLoginCommand) => Promise<void>;
  register: (creds: AuthRegisterCommand) => Promise<void>;
  logout: () => void;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserDto | undefined>(undefined);
  const [loading, setLoading] = useState(false);

  const isAuthenticated = Boolean(localStorage.getItem('accessToken')) && Boolean(user);

  const fetchUser = async () => {
    try {
      const res = await api.get<UserDto>('/users/me');
      setUser(res.data);
    } catch (e) {
      // ignore - user may not be available
      console.error('fetchUser error', e);
    }
  };

  useEffect(() => {
    if (localStorage.getItem('accessToken')) {
      void fetchUser();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setTokens = (auth: AuthResponseDto) => {
    if (auth.accessToken) localStorage.setItem('accessToken', auth.accessToken);
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
    setUser(undefined);
    // optional: redirect
    try {
      if (typeof window !== 'undefined') window.location.href = '/login';
    } catch {}
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, loading, login, register, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};

