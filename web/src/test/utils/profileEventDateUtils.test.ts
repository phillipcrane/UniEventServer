import { describe, expect, it } from 'vitest';
import { addDays, addMonths, endOfDayMs, parseDateOnly, startOfDayMs, toInputDateString } from '../../utils/dateUtils';
import {
    DEFAULT_EVENT_COVER_IMAGE_URL,
    getEventCoverImageUrl,
    getEventUrl,
    getOrganizerName,
} from '../../utils/eventUtils';
import { buildUsername, filterAndSortLikedEvents } from '../../utils/profileUtils';
import type { Event, Page, User } from '../../types';

function event(overrides: Partial<Event> = {}): Event {
    return {
        id: 'event-1',
        pageId: 'page-1',
        title: 'Friday Bar',
        startTime: '2026-05-07T18:00:00.000Z',
        description: '',
        ...overrides,
    } as Event;
}

describe('dateUtils', () => {
    it('parses date-only strings into local Date objects', () => {
        const parsed = parseDateOnly('2026-05-07');

        expect(parsed?.getFullYear()).toBe(2026);
        expect(parsed?.getMonth()).toBe(4);
        expect(parsed?.getDate()).toBe(7);
        expect(parseDateOnly('')).toBeUndefined();
        expect(parseDateOnly('not-a-date')).toBeUndefined();
    });

    it('calculates day boundaries and input date strings', () => {
        const date = new Date(2026, 4, 7, 13, 45, 10);

        expect(new Date(startOfDayMs(date)).getHours()).toBe(0);
        expect(new Date(endOfDayMs(date)).getHours()).toBe(23);
        expect(toInputDateString(date)).toBe('2026-05-07');
    });

    it('adds days and months without mutating the source date', () => {
        const date = new Date(2026, 4, 7);

        expect(toInputDateString(addDays(date, 3))).toBe('2026-05-10');
        expect(toInputDateString(addMonths(date, 2))).toBe('2026-07-07');
        expect(toInputDateString(date)).toBe('2026-05-07');
    });
});

describe('eventUtils', () => {
    it('resolves organizer names by explicit event fields before page lookup', () => {
        const pages: Page[] = [{ id: 'page-1', name: 'S-Huset', url: 'https://example.com/shuset', active: true }];

        expect(getOrganizerName(null, pages)).toBe('Unknown');
        expect(getOrganizerName(event({ organizerName: 'Explicit Organizer' } as Partial<Event>), pages)).toBe('Explicit Organizer');
        expect(getOrganizerName(event({ pageName: 'Embedded Page' } as Partial<Event>), pages)).toBe('Embedded Page');
        expect(getOrganizerName(event(), pages)).toBe('S-Huset');
        expect(getOrganizerName(event({ pageId: 'missing' }), pages)).toBe('Unknown');
    });

    it('returns explicit and fallback event URLs', () => {
        expect(getEventUrl('event-1', 'https://example.com/custom')).toBe('https://example.com/custom');
        expect(getEventUrl('event-1')).toBe('https://facebook.com/events/event-1');
    });

    it('returns explicit cover images or the default image', () => {
        expect(getEventCoverImageUrl(' https://example.com/cover.jpg ')).toBe('https://example.com/cover.jpg');
        expect(getEventCoverImageUrl('  ')).toBe(DEFAULT_EVENT_COVER_IMAGE_URL);
        expect(getEventCoverImageUrl(undefined)).toBe(DEFAULT_EVENT_COVER_IMAGE_URL);
    });
});

describe('profileUtils', () => {
    it('builds usernames from email, display name, or fallback', () => {
        expect(buildUsername({ email: 'alice@example.com' } as User)).toBe('alice');
        expect(buildUsername({ displayName: 'Alice Smith' } as User)).toBe('alice.smith');
        expect(buildUsername(null)).toBe('username');
    });

    it('filters liked events and sorts them by start time', () => {
        const events = [
            event({ id: 'liked-later', startTime: '2026-05-09T12:00:00.000Z' }),
            event({ id: 'unliked', startTime: '2026-05-07T12:00:00.000Z' }),
            event({ id: 'liked-sooner', startTime: '2026-05-08T12:00:00.000Z' }),
        ];

        expect(filterAndSortLikedEvents(events, ['liked-later', 'liked-sooner']).map((item) => item.id))
            .toEqual(['liked-sooner', 'liked-later']);
    });
});
