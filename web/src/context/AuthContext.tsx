/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  getCurrentUser,
  isTokenExpiredOrExpiringSoon,
  loginWithEmail,
  logout,
  onUserChanged,
  refreshSession,
  type AuthUser,
} from '../services/auth';
import { ensureCsrfToken } from '../services/csrf';
import { REFRESH_INTERVAL_MS, REFRESH_THRESHOLD_MS } from '../constants';
import { mapAuthError } from '../utils/authUtils';

type AuthContextType = {
  currentUser: AuthUser | null;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
  clearError: () => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// A context is a REACT-particular thing to share state and just data in general across various REACT components
// accross the entire app without having to pass state "props" (i.e. parameters, properties) down through 
// multiple levels of components (which can get messy and is called "prop drilling"). 
export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Restore session from HttpOnly cookies on page load.
    // Bootstrap a CSRF token for unauthenticated clients so state-changing
    // requests don't later fail with 403.
    void (async () => {
      try {
        await ensureCsrfToken();
      } catch {
        // ignore; refresh doesn't require CSRF
      }
      if (getCurrentUser()) {
        void refreshSession();
      }
    })();
    const unsubscribe = onUserChanged((user) => {
      setCurrentUser(user);
      setIsLoading(false);
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const handleFocus = () => {
      if (!currentUser) return;
      if (!isTokenExpiredOrExpiringSoon(REFRESH_THRESHOLD_MS)) return;
      refreshSession()
        .catch((err) => {
          setError(mapAuthError(err));
          return false;
        });
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [currentUser]);

  useEffect(() => {
    if (!currentUser) return undefined;

    const intervalId = window.setInterval(() => {
      if (!isTokenExpiredOrExpiringSoon(REFRESH_THRESHOLD_MS)) return;
      refreshSession()
        .catch((err) => {
          setError(mapAuthError(err));
          return false;
        });
    }, REFRESH_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, [currentUser]);

  const value = useMemo<AuthContextType>(() => ({
    currentUser,
    isLoading,
    error,
    login: async (email: string, password: string) => {
      setIsLoading(true);
      setError(null);
      try {
        const user = await loginWithEmail(email, password);
        setCurrentUser(user);
      } catch (err) {
        setError(mapAuthError(err));
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    logout: async () => {
      setIsLoading(true);
      setError(null);
      try {
        await logout();
        setCurrentUser(null);
      } catch (err) {
        setError(mapAuthError(err));
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    refreshSession: async () => {
      try {
        const refreshed = await refreshSession();
        if (!refreshed && currentUser) {
          setCurrentUser(null);
        }
        return refreshed;
      } catch (err) {
        setError(mapAuthError(err));
        return false;
      }
    },
    clearError: () => setError(null),
  }), [currentUser, error, isLoading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
