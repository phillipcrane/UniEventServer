import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SAVE_FEEDBACK_MS } from '../../constants';
import { useEventPage } from '../../hooks/useEventPage';
import type { Event, Page, User } from '../../types';

const mockGetEventById = vi.fn();
const mockLogout = vi.fn();

let mockCurrentUser: User | null = null;
let mockPages: Page[] = [];

vi.mock('../../services/dal', () => ({
    getEventById: (...args: unknown[]) => mockGetEventById(...args),
}));

vi.mock('../../services/auth', () => ({
    logout: () => mockLogout(),
}));

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({ currentUser: mockCurrentUser }),
}));

vi.mock('../../context/PagesContext', () => ({
    usePages: () => mockPages,
}));

function event(overrides: Partial<Event> = {}): Event {
    return {
        id: 'event-1',
        pageId: 'page-1',
        title: 'Friday Bar',
        startTime: '2099-05-07T18:00:00.000Z',
        description: 'Music at DTU',
        coverImageUrl: 'https://example.com/cover.jpg',
        ...overrides,
    } as Event;
}

describe('useEventPage', () => {
    beforeEach(() => {
        mockGetEventById.mockReset();
        mockLogout.mockReset();
        mockCurrentUser = null;
        mockPages = [];
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.useRealTimers();
    });

    it('loads an event by id and derives organizer and cover image data', async () => {
        mockPages = [{ id: 'page-1', name: 'S-Huset', url: 'https://example.com/shuset', active: true }];
        mockGetEventById.mockResolvedValueOnce(event());

        const { result } = renderHook(() => useEventPage('event-1'));

        expect(result.current.isLoading).toBe(true);
        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(mockGetEventById).toHaveBeenCalledWith('event-1');
        expect(result.current.event?.title).toBe('Friday Bar');
        expect(result.current.organizerName).toBe('S-Huset');
        expect(result.current.coverImageUrl).toBe('https://example.com/cover.jpg');
    });

    it('uses current user information for the profile label', async () => {
        mockCurrentUser = { uid: 'user-1', email: 'alice@example.com', displayName: 'Alice A.' } as User;
        mockGetEventById.mockResolvedValueOnce(event());

        const { result } = renderHook(() => useEventPage('event-1'));

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.currentUser).toBe(mockCurrentUser);
        expect(result.current.userLabel).toBe('Alice A.');
    });

    it('keeps a not-found event as null after loading', async () => {
        mockGetEventById.mockResolvedValueOnce(null);

        const { result } = renderHook(() => useEventPage('missing-event'));

        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.event).toBeNull();
        expect(result.current.organizerName).toBe('Unknown');
        expect(result.current.coverImageUrl).toBe('/dtuevent-logo.png');
    });

    it('does not fetch when no id is provided', () => {
        renderHook(() => useEventPage(undefined));

        expect(mockGetEventById).not.toHaveBeenCalled();
    });

    it('shows and clears save feedback after like changes', async () => {
        mockGetEventById.mockResolvedValueOnce(event());
        const { result } = renderHook(() => useEventPage('event-1'));
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        vi.useFakeTimers();
        act(() => {
            result.current.handleLikeToggle(true);
        });
        expect(result.current.saveFeedback).toBe('Saved to your profile.');

        act(() => {
            vi.advanceTimersByTime(SAVE_FEEDBACK_MS);
        });
        expect(result.current.saveFeedback).toBe('');
    });

    it('logs out and resets signing-out state', async () => {
        mockLogout.mockResolvedValueOnce(undefined);
        mockGetEventById.mockResolvedValueOnce(event());
        const { result } = renderHook(() => useEventPage('event-1'));
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        await act(async () => {
            await result.current.handleSignOut();
        });

        expect(mockLogout).toHaveBeenCalled();
        expect(result.current.isSigningOut).toBe(false);
    });

    it('maps logout errors to console output without throwing', async () => {
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        mockLogout.mockRejectedValueOnce(Object.assign(new Error('Invalid credentials.'), { status: 401 }));
        mockGetEventById.mockResolvedValueOnce(event());
        const { result } = renderHook(() => useEventPage('event-1'));
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        await act(async () => {
            await result.current.handleSignOut();
        });

        expect(consoleError).toHaveBeenCalledWith('Invalid email or password.');
        expect(result.current.isSigningOut).toBe(false);
    });
});
