/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { getPages } from '../services/dal';
import type { Page } from '../types';

// A context is a REACT-particular way to share state across various REACT components without
// having to manually pass down variables through components (prop drilling). Pages are
// static-ish public data fetched once at app startup - making them global context means
// any hook or component can read the list without triggering a redundant network request.

const PagesContext = createContext<Page[]>([]);

export function PagesProvider({ children }: { children: ReactNode }) {
    const [pages, setPages] = useState<Page[]>([]);

    useEffect(() => {
        getPages().then(setPages).catch(() => { /* stay empty on network error */ });
    }, []);

    return <PagesContext.Provider value={pages}>{children}</PagesContext.Provider>;
}

export function usePages(): Page[] {
    return useContext(PagesContext);
}
