import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { isTokenExpiredOrExpiringSoon, onAuthUserChanged, refreshTokens, type AuthUser } from '../services/auth';

// A context is a REACT-particular thing to share state and just data in general across various REACT components
// accross the entire app without having to pass state "props" (i.e. parameters, properties) down through 
// multiple levels of components (which can get messy and is called "prop drilling"). 

type AuthContextValue = {
    currentUser: AuthUser | null;
};

const AuthContext = createContext<AuthContextValue>({ currentUser: null });

// Check every 4 minutes - well within the 5-minute prompt-cache window and
// short enough to catch a 24-hour access token before it expires.
const REFRESH_INTERVAL_MS = 4 * 60 * 1000;
// Trigger a proactive refresh when the token has less than 5 minutes left.
const REFRESH_THRESHOLD_MS = 5 * 60 * 1000;

export function AuthProvider({ children }: { children: ReactNode }) {
    const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);

    useEffect(() => {
        // Restore session from HttpOnly cookies on page load.
        // If the refresh cookie is valid the server issues fresh tokens and
        // populates in-memory state; if not, the user stays logged out.
        refreshTokens();
        const unsubscribe = onAuthUserChanged(setCurrentUser);
        return unsubscribe;
    }, []);

    useEffect(() => {
        // Proactively refresh the access token before it expires so the user
        // never gets a surprise 401 mid-session. Empty deps keeps the interval
        // stable - getCurrentUser() reads the latest state on each tick.
        async function checkAndRefresh() {
            if (isTokenExpiredOrExpiringSoon(REFRESH_THRESHOLD_MS)) {
                await refreshTokens();
            }
        }

        checkAndRefresh();
        const interval = setInterval(checkAndRefresh, REFRESH_INTERVAL_MS);
        return () => clearInterval(interval);
    }, []);

    return <AuthContext.Provider value={{ currentUser }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    return useContext(AuthContext);
}
