import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EventCard } from '../../components/EventCard';
import type { Event } from '../../types';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../components/LikeButton', () => ({
    LikeButton: () => <button type="button" aria-label="Like">Like</button>,
}));

vi.mock('../../components/FacebookLinkButton', () => ({
    FacebookLinkButton: ({ event }: { event: Event }) => (
        <a href={event.eventURL ?? '#'}>Open on Facebook</a>
    ),
}));

const baseEvent: Event = {
    id: 'event-1',
    pageId: 'page-1',
    title: 'Robotics Night',
    description: 'Build and test robots with other students.',
    startTime: '2026-06-01T18:00:00.000Z',
    place: { name: 'Oticon Hall' },
    coverImageUrl: 'https://example.com/cover.jpg',
    eventURL: 'https://facebook.com/events/event-1',
    createdAt: '2026-05-05T12:00:00.000Z',
    updatedAt: '2026-05-05T12:00:00.000Z',
};

describe('EventCard', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-05-06T12:00:00.000Z'));
        mockNavigate.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('renders event details and navigates when the collapsed card is clicked', () => {
        render(<EventCard event={baseEvent} />);

        expect(screen.getByText('Robotics Night')).toBeInTheDocument();
        expect(screen.getByText('Oticon Hall')).toBeInTheDocument();
        expect(screen.getByText('New event')).toBeInTheDocument();

        fireEvent.click(screen.getByText('Robotics Night'));

        expect(mockNavigate).toHaveBeenCalledWith('/events/event-1');
    });

    it('uses a fallback cover image when the original image fails to load', () => {
        render(<EventCard event={baseEvent} />);

        const image = screen.getByRole('img', { name: 'Robotics Night' }) as HTMLImageElement;
        expect(image.src).toBe('https://example.com/cover.jpg');

        fireEvent.error(image);

        expect(image.src).toContain('/dtuevent-logo.png');
    });

    it('renders Location TBA when no place name exists', () => {
        render(<EventCard event={{ ...baseEvent, place: undefined }} />);

        expect(screen.getByText('Location TBA')).toBeInTheDocument();
    });
});
