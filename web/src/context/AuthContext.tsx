import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { onAuthUserChanged, type AuthUser } from '../services/auth';

// A context is a REACT-particular thing to share state and just data in general across various REACT components
// accross the entire app without having to pass state "props" (i.e. parameters, properties) down through 
// multiple levels of components (which can get messy and is called "prop drilling"). 

type AuthContextValue = {
    currentUser: AuthUser | null;
};

const AuthContext = createContext<AuthContextValue>({ currentUser: null });

export function AuthProvider({ children }: { children: ReactNode }) {
    const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);

    useEffect(() => {
        const unsubscribe = onAuthUserChanged(setCurrentUser);
        return unsubscribe;
    }, []);

    return <AuthContext.Provider value={{ currentUser }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    return useContext(AuthContext);
}
