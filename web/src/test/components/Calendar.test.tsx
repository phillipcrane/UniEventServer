import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CalendarView } from '../../components/Calendar';
import type { Event } from '../../types';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

const events: Event[] = [
    {
        id: 'event-1',
        pageId: 'page-1',
        title: 'Robotics Night',
        startTime: '2026-05-06T18:00:00.000Z',
        endTime: '2026-05-06T20:00:00.000Z',
        createdAt: '2026-05-01T00:00:00.000Z',
        updatedAt: '2026-05-01T00:00:00.000Z',
    },
    {
        id: 'event-2',
        pageId: 'page-1',
        title: 'Festival Week',
        startTime: '2026-05-07T18:00:00.000Z',
        endTime: '2026-05-09T20:00:00.000Z',
        createdAt: '2026-05-01T00:00:00.000Z',
        updatedAt: '2026-05-01T00:00:00.000Z',
    },
];

describe('CalendarView', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-05-06T12:00:00.000Z'));
        mockNavigate.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('renders current month events and navigates to an event', () => {
        render(<CalendarView events={events} />);

        expect(screen.getByText(/May 2026/)).toBeInTheDocument();
        expect(screen.getByText('Robotics Night')).toBeInTheDocument();
        expect(screen.getAllByText('Festival Week')).toHaveLength(3);

        fireEvent.click(screen.getByText('Robotics Night'));

        expect(mockNavigate).toHaveBeenCalledWith('/events/event-1');
    });

    it('switches between month and week views', () => {
        render(<CalendarView events={events} />);

        fireEvent.click(screen.getByRole('button', { name: 'Week' }));

        expect(screen.getByText(/Week of/)).toBeInTheDocument();
        expect(screen.getByText('Robotics Night')).toBeInTheDocument();
    });
});
