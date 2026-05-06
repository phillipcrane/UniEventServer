import { fireEvent, render, screen } from '@testing-library/react';
import { useRef } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useClickOutside } from '../../hooks/useClickOutside';

function Harness({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
    const ref = useRef<HTMLDivElement | null>(null);
    useClickOutside(ref, isOpen, onClose);

    return (
        <div>
            <div ref={ref}>
                <button type="button">Inside</button>
            </div>
            <button type="button">Outside</button>
        </div>
    );
}

describe('useClickOutside', () => {
    const onClose = vi.fn();

    beforeEach(() => {
        onClose.mockReset();
    });

    it('does not close when clicking inside the referenced element', () => {
        render(<Harness isOpen onClose={onClose} />);

        fireEvent.mouseDown(screen.getByRole('button', { name: 'Inside' }));

        expect(onClose).not.toHaveBeenCalled();
    });

    it('closes when clicking outside the referenced element', () => {
        render(<Harness isOpen onClose={onClose} />);

        fireEvent.mouseDown(screen.getByRole('button', { name: 'Outside' }));

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('does not subscribe while closed', () => {
        render(<Harness isOpen={false} onClose={onClose} />);

        fireEvent.mouseDown(screen.getByRole('button', { name: 'Outside' }));

        expect(onClose).not.toHaveBeenCalled();
    });
});
