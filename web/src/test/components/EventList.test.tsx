import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { EventList } from '../../components/EventList';
import type { Event } from '../../types';

vi.mock('../../components/EventCard', () => ({
    EventCard: ({ event }: { event: Event }) => <article aria-label={event.title}>{event.title}</article>,
}));

const events: Event[] = [
    {
        id: 'event-1',
        pageId: 'page-1',
        title: 'Robotics Night',
        startTime: '2026-06-01T18:00:00.000Z',
        createdAt: '2026-05-01T00:00:00.000Z',
        updatedAt: '2026-05-01T00:00:00.000Z',
    },
    {
        id: 'event-2',
        pageId: 'page-1',
        title: 'Friday Bar',
        startTime: '2026-06-02T18:00:00.000Z',
        createdAt: '2026-05-01T00:00:00.000Z',
        updatedAt: '2026-05-01T00:00:00.000Z',
    },
];

describe('EventList', () => {
    it('renders an empty state when there are no events', () => {
        render(<EventList list={[]} />);

        expect(screen.getByText('No events found for this page.')).toBeInTheDocument();
    });

    it('renders every event in the list', () => {
        render(<EventList list={events} />);

        expect(screen.getByRole('article', { name: 'Robotics Night' })).toBeInTheDocument();
        expect(screen.getByRole('article', { name: 'Friday Bar' })).toBeInTheDocument();
    });
});
