import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
    loginWithEmail,
    logout,
    mapAuthError,
    onAuthUserChanged,
    refreshSession,
    type AuthUser,
} from '../services/auth';

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

export function AuthProvider({ children }: { children: ReactNode }) {
    const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const unsubscribe = onAuthUserChanged((user) => {
            setCurrentUser(user);
            setIsLoading(false);
        });
        return unsubscribe;
    }, []);

    useEffect(() => {
        if (typeof window === 'undefined') return undefined;
        const handleFocus = () => {
            if (!currentUser) return;
            refreshSession()
                .catch((err) => {
                    setError(mapAuthError(err, 'general'));
                    return false;
                });
        };

        window.addEventListener('focus', handleFocus);
        return () => window.removeEventListener('focus', handleFocus);
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
                setError(mapAuthError(err, 'login'));
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
                setError(mapAuthError(err, 'general'));
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
                setError(mapAuthError(err, 'general'));
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
