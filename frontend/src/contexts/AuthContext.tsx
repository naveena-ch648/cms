import { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import apiClient from '../api/client';
import type { AuthUser, TokenResponse } from '../types/models';
import type { ApiResponse } from '../types/api';

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<ApiResponse<TokenResponse>>('/auth/login', {
      email,
      password,
    });
    const tokenData = response.data.data!;
    localStorage.setItem('accessToken', tokenData.accessToken);
    localStorage.setItem('refreshToken', tokenData.refreshToken);
    const u = tokenData.user!;
    setUser({
      id: u.id,
      email: u.email,
      firstName: u.firstName,
      lastName: u.lastName,
      status: u.status ?? 'ACTIVE',
      organizationId: u.organizationId,
      organizationName: u.organizationName,
      organizationRole: u.organizationRole?.name ?? '',
    });
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Ignore errors during logout
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
    }
  }, []);

  const refreshAuth = useCallback(async () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setIsLoading(false);
      return;
    }
    try {
      const response = await apiClient.get<ApiResponse<Record<string, string>>>('/auth/me');
      const data = response.data.data!;
      setUser({
        id: data.id ?? '',
        email: data.email ?? '',
        firstName: data.firstName ?? '',
        lastName: data.lastName ?? '',
        status: data.status ?? 'ACTIVE',
        organizationId: data.organizationId ?? '',
        organizationName: data.organizationName ?? '',
        organizationRole: data.organizationRole ?? '',
      });
    } catch {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshAuth();
  }, [refreshAuth]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout, refreshAuth }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
