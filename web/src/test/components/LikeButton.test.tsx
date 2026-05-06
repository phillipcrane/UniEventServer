import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LikeButton } from '../../components/LikeButton';
import type { Event, User } from '../../types';

const mockNavigate = vi.fn();
const mockToggle = vi.fn();

const state = vi.hoisted(() => ({
    currentUser: null as User | null,
    likedIds: new Set<string>(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({ currentUser: state.currentUser }),
}));

vi.mock('../../context/LikesContext', () => ({
    useLikes: () => ({
        isLiked: (eventId: string) => state.likedIds.has(eventId),
        toggle: (...args: unknown[]) => mockToggle(...args),
        likedIds: state.likedIds,
    }),
}));

const event: Event = {
    id: 'event-1',
    pageId: 'page-1',
    title: 'Friday Bar',
    startTime: '2026-06-01T18:00:00.000Z',
    createdAt: '2026-05-01T00:00:00.000Z',
    updatedAt: '2026-05-01T00:00:00.000Z',
};

describe('LikeButton', () => {
    beforeEach(() => {
        state.currentUser = null;
        state.likedIds = new Set();
        mockNavigate.mockReset();
        mockToggle.mockReset();
    });

    it('redirects anonymous users to login instead of toggling likes', () => {
        render(<LikeButton event={event} />);

        fireEvent.click(screen.getByRole('button', { name: 'Like' }));

        expect(mockNavigate).toHaveBeenCalledWith('/login');
        expect(mockToggle).not.toHaveBeenCalled();
    });

    it('renders liked state for signed-in users', () => {
        state.currentUser = { uid: 'user-1', username: 'Alice', email: 'alice@example.com' };
        state.likedIds = new Set(['event-1']);

        render(<LikeButton event={event} />);

        const button = screen.getByRole('button', { name: 'Liked' });
        expect(button).toHaveAttribute('aria-pressed', 'true');
    });

    it('toggles likes and reports the new state', async () => {
        const onToggleChange = vi.fn();
        state.currentUser = { uid: 'user-1', username: 'Alice', email: 'alice@example.com' };
        mockToggle.mockResolvedValueOnce(true);

        render(<LikeButton event={event} onToggleChange={onToggleChange} />);

        fireEvent.click(screen.getByRole('button', { name: 'Like' }));

        await waitFor(() => {
            expect(mockToggle).toHaveBeenCalledWith('event-1');
        });
        expect(onToggleChange).toHaveBeenCalledWith(true);
    });
});
