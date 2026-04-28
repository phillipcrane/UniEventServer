import { useEffect, useState, useMemo } from 'react';
import { getEvents, getPages } from '../services/dal';
import { signOutCurrentUser } from '../handlers/logout';
import { redirectToFacebookAuth } from '../handlers/facebookLogin';
import { mapAuthError } from '../utils/authUtils';
import { parseDateOnly, startOfDayMs, endOfDayMs } from '../utils/dateUtils';
import { useAuth } from '../context/AuthContext';
import { DEBOUNCE_MS } from '../constants';
import type { Event as EventType, Page, SortMode } from '../types';

const FB_ERROR_MESSAGES: Record<string, string> = {
    access_denied: 'Facebook access was denied. Please try again and accept the required permissions.',
    server_error: 'Facebook encountered a server error. Please try again later.',
    temporarily_unavailable: 'Facebook is temporarily unavailable. Please try again later.',
};

function getCreatedMs(e: EventType): number {
    type LegacyEvent = EventType & { createdTime?: string; postedTime?: string; insertedAt?: string; addedAt?: string };
    const le = e as LegacyEvent;
    const maybe = le.createdTime ?? le.createdAt ?? le.postedTime ?? le.insertedAt ?? le.addedAt ?? le.startTime;
    const ms = Date.parse(maybe);
    return isNaN(ms) ? new Date(e.startTime).getTime() : ms;
}

export function useMainPage() {
    const { currentUser } = useAuth();
    const [pages, setPages] = useState<Page[]>([]);
    const [events, setEvents] = useState<EventType[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [isSigningOut, setIsSigningOut] = useState(false);
    const [fbConnecting, setFbConnecting] = useState(false);
    const [fbMessage, setFbMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null);

    const [pageIds, setPageIds] = useState<string[]>([]);
    const [query, setQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');
    const [fromDate, setFromDate] = useState('');
    const [toDate, setToDate] = useState('');
    const [sortMode, setSortMode] = useState<SortMode>('upcoming');
    const [viewMode, setViewMode] = useState<'list' | 'calendar'>('list');

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                setLoading(true);
                const [page, event] = await Promise.all([getPages(), getEvents()]);
                if (cancelled) return;
                setPages(page);
                setEvents(event);
            } catch (err) {
                if (cancelled) return;
                setError(err instanceof Error && err.message ? err.message : 'Failed to load data');
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, []);

    useEffect(() => {
        const id = setTimeout(() => setDebouncedQuery(query.trim().toLowerCase()), DEBOUNCE_MS);
        return () => clearTimeout(id);
    }, [query]);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const success = params.get('success');
        const fbError = params.get('error');
        if (success === 'true') {
            const linkedPages = params.get('pages');
            setFbMessage({ kind: 'success', text: `Facebook connected - ${linkedPages ?? '0'} page(s) linked.` });
            window.history.replaceState({}, '', window.location.pathname);
        } else if (fbError) {
            const friendlyError = FB_ERROR_MESSAGES[fbError] ?? 'Could not connect Facebook. Please try again.';
            setFbMessage({ kind: 'error', text: friendlyError });
            window.history.replaceState({}, '', window.location.pathname);
        }
    }, []);

    async function handleFacebookConnect() {
        try {
            setFbConnecting(true);
            setFbMessage(null);
            await redirectToFacebookAuth();
        } catch (err) {
            setFbMessage({ kind: 'error', text: err instanceof Error ? err.message : 'Could not start Facebook login.' });
        } finally {
            setFbConnecting(false);
        }
    }

    async function handleSignOut() {
        try {
            setIsSigningOut(true);
            setError('');
            await signOutCurrentUser();
        } catch (err) {
            setError(mapAuthError(err));
        } finally {
            setIsSigningOut(false);
        }
    }

    const userLabel = currentUser?.username || currentUser?.email || 'My Profile';

    const filteredByPage = useMemo(
        () => pageIds.length > 0 ? events.filter((e) => pageIds.includes(e.pageId)) : events,
        [events, pageIds],
    );

    const textFiltered = useMemo(
        () => debouncedQuery
            ? filteredByPage.filter((event) =>
                ((event.title || '') + ' ' + (event.description || '') + ' ' + (event.place?.name || ''))
                    .toLowerCase()
                    .includes(debouncedQuery)
              )
            : filteredByPage,
        [filteredByPage, debouncedQuery],
    );

    const fromObj = useMemo(() => parseDateOnly(fromDate), [fromDate]);
    const toObj = useMemo(() => parseDateOnly(toDate), [toDate]);
    const invalidRange = !!(fromObj && toObj && toObj < fromObj);

    const dateFiltered = useMemo(() => textFiltered.filter((event) => {
        const eventMs = new Date(event.startTime).getTime();
        if (fromObj && eventMs < startOfDayMs(fromObj)) return false;
        const effectiveToObj = invalidRange ? undefined : toObj;
        if (effectiveToObj && eventMs > endOfDayMs(effectiveToObj)) return false;
        return true;
    }), [textFiltered, fromObj, toObj, invalidRange]);

    const list = useMemo(() => {
        let result = [...dateFiltered];
        if (sortMode !== 'all') {
            const now = Date.now();
            result = result.filter((event) => new Date(event.startTime).getTime() >= now);
        }
        if (sortMode === 'upcoming') {
            result.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
        } else if (sortMode === 'newest') {
            result.sort((a, b) => getCreatedMs(b) - getCreatedMs(a));
        }
        return result;
    }, [dateFiltered, sortMode]);

    return {
        currentUser,
        pages,
        list,
        loading,
        error,
        isSigningOut,
        fbConnecting,
        fbMessage,
        pageIds, setPageIds,
        query, setQuery,
        fromDate, setFromDate,
        toDate, setToDate,
        sortMode, setSortMode,
        viewMode, setViewMode,
        count: list.length,
        invalidRange,
        userLabel,
        handleFacebookConnect,
        handleSignOut,
    };
}
