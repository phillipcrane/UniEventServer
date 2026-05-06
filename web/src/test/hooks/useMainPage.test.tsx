import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useMainPage } from '../../hooks/useMainPage';
import type { Event as EventType, Page } from '../../types';

const mockGetEvents = vi.fn();
const mockLogout = vi.fn();
const mockGetFacebookAuthUrl = vi.fn();
const mockMapAuthError = vi.fn();

let mockCurrentUser: unknown = null;
let mockPages: Page[] = [];

vi.mock('../../services/dal', () => ({
    getEvents: () => mockGetEvents(),
}));

vi.mock('../../services/auth', () => ({
    logout: () => mockLogout(),
}));

vi.mock('../../services/facebook', () => ({
    getFacebookAuthUrl: () => mockGetFacebookAuthUrl(),
}));

vi.mock('../../utils/authUtils', () => ({
    mapAuthError: (...args: unknown[]) => mockMapAuthError(...args),
}));

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({ currentUser: mockCurrentUser }),
}));

vi.mock('../../context/PagesContext', () => ({
    usePages: () => mockPages,
}));

function event(overrides: Partial<EventType>): EventType {
    return {
        id: 'event-1',
        pageId: 'page-1',
        title: 'Friday Bar',
        description: 'Live music',
        startTime: '2099-05-07T18:00:00.000Z',
        createdAt: '2099-05-01T12:00:00.000Z',
        place: { name: 'S-Huset' },
        ...overrides,
    } as EventType;
}

describe('useMainPage', () => {
    beforeEach(() => {
        mockGetEvents.mockReset();
        mockLogout.mockReset();
        mockGetFacebookAuthUrl.mockReset();
        mockMapAuthError.mockReset();
        mockCurrentUser = null;
        mockPages = [];
        window.history.replaceState({}, '', '/');
    });

    afterEach(() => {
        window.history.replaceState({}, '', '/');
    });

    it('loads events and exposes page context data', async () => {
        const pages = [{ id: 'page-1', name: 'S-Huset', url: 'https://example.com/shuset', active: true }];
        mockPages = pages;
        mockGetEvents.mockResolvedValueOnce([
            event({ id: 'future-2', title: 'Later Event', startTime: '2099-05-08T18:00:00.000Z' }),
            event({ id: 'future-1', title: 'Soon Event', startTime: '2099-05-07T18:00:00.000Z' }),
        ]);

        const { result } = renderHook(() => useMainPage());

        await waitFor(() => expect(result.current.loading).toBe(false));
        expect(result.current.pages).toBe(pages);
        expect(result.current.count).toBe(2);
        expect(result.current.list.map((item) => item.id)).toEqual(['future-1', 'future-2']);
    });

    it('filters by selected page, debounced search text, and date range', async () => {
        mockGetEvents.mockResolvedValueOnce([
            event({ id: 'match', pageId: 'page-2', title: 'Drone Workshop', startTime: '2099-05-09T12:00:00.000Z' }),
            event({ id: 'wrong-page', pageId: 'page-1', title: 'Drone Workshop', startTime: '2099-05-09T12:00:00.000Z' }),
            event({ id: 'wrong-text', pageId: 'page-2', title: 'Concert', startTime: '2099-05-09T12:00:00.000Z' }),
            event({ id: 'wrong-date', pageId: 'page-2', title: 'Drone Workshop', startTime: '2099-05-12T12:00:00.000Z' }),
        ]);

        const { result } = renderHook(() => useMainPage());
        await waitFor(() => expect(result.current.loading).toBe(false));

        act(() => {
            result.current.setPageIds(['page-2']);
            result.current.setQuery('drone');
            result.current.setFromDate('2099-05-09');
            result.current.setToDate('2099-05-10');
        });

        await waitFor(() => {
            expect(result.current.list.map((item) => item.id)).toEqual(['match']);
        });
    });

    it('reports invalid date ranges without applying the to-date cutoff', async () => {
        mockGetEvents.mockResolvedValueOnce([
            event({ id: 'future', startTime: '2099-05-09T12:00:00.000Z' }),
            event({ id: 'past', startTime: '2099-05-03T12:00:00.000Z' }),
        ]);

        const { result } = renderHook(() => useMainPage());
        await waitFor(() => expect(result.current.loading).toBe(false));

        act(() => {
            result.current.setFromDate('2099-05-08');
            result.current.setToDate('2099-05-07');
        });

        expect(result.current.invalidRange).toBe(true);
        expect(result.current.list.map((item) => item.id)).toEqual(['future']);
    });

    it('reads Facebook connection status from URL params and clears them', async () => {
        window.history.replaceState({}, '', '/?success=true&pages=3');
        mockGetEvents.mockResolvedValueOnce([]);

        const { result } = renderHook(() => useMainPage());

        await waitFor(() => {
            expect(result.current.fbMessage).toEqual({
                kind: 'success',
                text: 'Facebook connected - 3 page(s) linked.',
            });
        });
        expect(window.location.search).toBe('');
    });

    it('maps logout failures into the page error state', async () => {
        const error = new Error('logout failed');
        mockCurrentUser = { uid: 'u1', email: 'alice@example.com', username: 'alice' };
        mockGetEvents.mockResolvedValueOnce([]);
        mockLogout.mockRejectedValueOnce(error);
        mockMapAuthError.mockReturnValueOnce('Could not sign out.');

        const { result } = renderHook(() => useMainPage());
        await waitFor(() => expect(result.current.loading).toBe(false));

        await act(async () => {
            await result.current.handleSignOut();
        });

        expect(mockMapAuthError).toHaveBeenCalledWith(error);
        expect(result.current.error).toBe('Could not sign out.');
        expect(result.current.isSigningOut).toBe(false);
    });
});
