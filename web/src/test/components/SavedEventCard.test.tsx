import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { SavedEventCard } from '../../components/SavedEventCard';
import type { Event } from '../../types';

vi.mock('../../components/LikeButton', () => ({
    LikeButton: ({ compact }: { compact?: boolean }) => (
        <button type="button" aria-label={compact ? 'Compact like' : 'Like'}>
            Like
        </button>
    ),
}));

const event: Event = {
    id: 'event-1',
    pageId: 'page-1',
    title: 'Saved Robotics Night',
    startTime: '2026-06-01T18:00:00.000Z',
    place: { name: 'Oticon Hall' },
    coverImageUrl: 'https://example.com/saved-cover.jpg',
    createdAt: '2026-05-01T00:00:00.000Z',
    updatedAt: '2026-05-01T00:00:00.000Z',
};

describe('SavedEventCard', () => {
    it('renders saved event details and links to the event page', () => {
        render(
            <MemoryRouter>
                <SavedEventCard event={event} />
            </MemoryRouter>
        );

        expect(screen.getByRole('link', { name: 'Open event Saved Robotics Night' })).toHaveAttribute(
            'href',
            '/events/event-1'
        );
        expect(screen.getByRole('link', { name: 'Saved Robotics Night' })).toHaveAttribute('href', '/events/event-1');
        expect(screen.getByRole('img', { name: 'Saved Robotics Night' })).toHaveAttribute(
            'src',
            'https://example.com/saved-cover.jpg'
        );
        expect(screen.getByText('Oticon Hall')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Compact like' })).toBeInTheDocument();
    });

    it('renders fallback location when the event has no place', () => {
        render(
            <MemoryRouter>
                <SavedEventCard event={{ ...event, place: undefined, coverImageUrl: undefined }} />
            </MemoryRouter>
        );

        expect(screen.getByText('Location TBA')).toBeInTheDocument();
        expect(screen.queryByRole('img', { name: 'Saved Robotics Night' })).not.toBeInTheDocument();
    });
});
