import { act, fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ShareButton } from '../../components/ShareButton';
import { SHARE_FEEDBACK_MS } from '../../constants';
import type { Event } from '../../types';

const event: Event = {
    id: 'event-1',
    pageId: 'page-1',
    title: 'Friday Bar',
    startTime: '2026-06-01T18:00:00.000Z',
    eventURL: 'https://facebook.com/events/event-1',
    createdAt: '2026-05-01T00:00:00.000Z',
    updatedAt: '2026-05-01T00:00:00.000Z',
};

describe('ShareButton', () => {
    beforeEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
        Reflect.deleteProperty(navigator, 'share');
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true,
            value: undefined,
        });
    });

    it('uses the native share API when available', async () => {
        const share = vi.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, 'share', {
            configurable: true,
            value: share,
        });

        render(<ShareButton event={event} />);

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: 'Share event' }));
        });

        expect(share).toHaveBeenCalledWith({
            title: 'Friday Bar',
            text: 'Check out this event on UniEvent',
            url: 'https://facebook.com/events/event-1',
        });
    });

    it('copies the event link when native share is unavailable', async () => {
        vi.useFakeTimers();
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true,
            value: { writeText },
        });

        render(<ShareButton event={event} />);

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: 'Share event' }));
        });

        expect(writeText).toHaveBeenCalledWith('https://facebook.com/events/event-1');
        expect(screen.getByRole('button', { name: 'Share event' })).toHaveTextContent('Copied');

        act(() => {
            vi.advanceTimersByTime(SHARE_FEEDBACK_MS);
        });
        expect(screen.getByRole('button', { name: 'Share event' })).toHaveTextContent('Share');
    });

    it('opens the event URL when share and clipboard are unavailable', async () => {
        const open = vi.spyOn(window, 'open').mockImplementation(() => null);

        render(<ShareButton event={event} />);

        await act(async () => {
            fireEvent.click(screen.getByRole('button', { name: 'Share event' }));
        });

        expect(open).toHaveBeenCalledWith('https://facebook.com/events/event-1', '_blank', 'noopener,noreferrer');
    });
});
