import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FilterBar } from '../../components/FilterBar';
import type { Page, SortMode } from '../../types';

const pages: Page[] = [
    { id: 'page-1', name: 'DTU Robotics', url: 'https://example.com/robotics', active: true },
    { id: 'page-2', name: 'S-Huset', url: 'https://example.com/shuset', active: true },
];

function renderFilterBar(overrides: Partial<Parameters<typeof FilterBar>[0]> = {}) {
    const props = {
        pages,
        pageIds: [] as string[],
        setPageIds: vi.fn(),
        query: '',
        setQuery: vi.fn(),
        fromDate: '',
        setFromDate: vi.fn(),
        toDate: '',
        setToDate: vi.fn(),
        count: 12,
        sortMode: 'upcoming' as SortMode,
        setSortMode: vi.fn(),
        ...overrides,
    };

    render(<FilterBar {...props} />);
    return props;
}

describe('FilterBar', () => {
    beforeEach(() => {
        vi.useRealTimers();
    });

    it('updates and clears the search query', () => {
        const props = renderFilterBar({ query: 'robotics' });

        fireEvent.change(screen.getByLabelText('Search'), { target: { value: 'drone' } });
        fireEvent.click(screen.getByRole('button', { name: 'Clear search' }));

        expect(props.setQuery).toHaveBeenCalledWith('drone');
        expect(props.setQuery).toHaveBeenCalledWith('');
        expect(screen.getByLabelText('Matching events')).toHaveValue('12 results');
    });

    it('applies and clears date range presets', () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-05-06T12:00:00.000Z'));
        const props = renderFilterBar({ fromDate: '2026-05-06', toDate: '2026-05-13' });

        fireEvent.click(screen.getByRole('button', { name: 'Next 30 days' }));

        expect(props.setFromDate).toHaveBeenCalledWith('2026-05-06');
        expect(props.setToDate).toHaveBeenCalledWith('2026-06-05');

        fireEvent.click(screen.getByRole('button', { name: 'Clear date range' }));
        expect(props.setFromDate).toHaveBeenCalledWith('');
        expect(props.setToDate).toHaveBeenCalledWith('');
    });

    it('shows active filters and clears all filters', () => {
        const props = renderFilterBar({
            pageIds: ['page-1'],
            query: 'robot',
            fromDate: '2026-05-06',
            toDate: '2026-05-13',
            sortMode: 'newest',
        });

        expect(screen.getByText('Search: "robot"')).toBeInTheDocument();
        expect(screen.getByText('Organizer: DTU Robotics')).toBeInTheDocument();
        expect(screen.getByText('Sort: Recently added')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Clear all' }));

        expect(props.setQuery).toHaveBeenCalledWith('');
        expect(props.setPageIds).toHaveBeenCalledWith([]);
        expect(props.setFromDate).toHaveBeenCalledWith('');
        expect(props.setToDate).toHaveBeenCalledWith('');
        expect(props.setSortMode).toHaveBeenCalledWith('upcoming');
    });
});
