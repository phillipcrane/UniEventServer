import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MultiSelectFilter } from '../../components/MultiSelectFilter';
import type { Page } from '../../types';

const pages: Page[] = [
    { id: 'page-1', name: 'DTU Robotics', url: 'https://example.com/robotics', active: true },
    { id: 'page-2', name: 'S-Huset', url: 'https://example.com/shuset', active: true },
];

describe('MultiSelectFilter', () => {
    const onSelectionChange = vi.fn();

    beforeEach(() => {
        cleanup();
        onSelectionChange.mockReset();
    });

    it('shows an empty state when no pages are available', () => {
        render(<MultiSelectFilter pages={[]} selectedIds={[]} onSelectionChange={onSelectionChange} />);

        fireEvent.click(screen.getByRole('button', { name: 'Toggle organizer filter' }));

        expect(screen.getByText('No organizers available')).toBeInTheDocument();
    });

    it('adds and removes selected organizers', () => {
        render(<MultiSelectFilter pages={pages} selectedIds={[]} onSelectionChange={onSelectionChange} />);

        fireEvent.click(screen.getByRole('button', { name: 'Toggle organizer filter' }));
        fireEvent.click(screen.getByLabelText('DTU Robotics'));

        expect(onSelectionChange).toHaveBeenCalledWith(['page-1']);

        onSelectionChange.mockReset();
        cleanup();
        render(<MultiSelectFilter pages={pages} selectedIds={['page-1']} onSelectionChange={onSelectionChange} />);

        fireEvent.click(screen.getByRole('button', { name: 'Toggle organizer filter' }));
        fireEvent.click(screen.getByLabelText('DTU Robotics'));

        expect(onSelectionChange).toHaveBeenCalledWith([]);
    });

    it('removes a selected tag without opening duplicate selections', () => {
        render(<MultiSelectFilter pages={pages} selectedIds={['page-1', 'page-2']} onSelectionChange={onSelectionChange} />);

        fireEvent.click(screen.getByRole('button', { name: 'Remove DTU Robotics' }));

        expect(onSelectionChange).toHaveBeenCalledWith(['page-2']);
    });

    it('closes the menu when clicking outside', () => {
        render(
            <div>
                <MultiSelectFilter pages={pages} selectedIds={[]} onSelectionChange={onSelectionChange} />
                <button type="button">Outside</button>
            </div>,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Toggle organizer filter' }));
        expect(screen.getByText('DTU Robotics')).toBeInTheDocument();

        fireEvent.mouseDown(screen.getByRole('button', { name: 'Outside' }));

        expect(screen.queryByText('DTU Robotics')).not.toBeInTheDocument();
    });
});
