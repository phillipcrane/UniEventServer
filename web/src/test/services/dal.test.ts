import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getEventById, getEvents, getPages } from '../../services/dal';

const mockFetch = vi.fn();

function jsonResponse(body: unknown, status = 200, statusText = 'OK'): Response {
    return new Response(JSON.stringify(body), {
        status,
        statusText,
        headers: { 'Content-Type': 'application/json' },
    });
}

beforeEach(() => {
    mockFetch.mockReset();
    vi.stubGlobal('fetch', mockFetch);
});

describe('dal service', () => {
    it('maps fetched pages into app page format', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            content: [
                { id: 'p-1', name: 'S-Huset', url: 'https://example.com/shuset', active: true },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 100,
            hasNext: false,
            hasPrevious: false,
        }));

        const pages = await getPages();

        expect(pages).toEqual([
            { id: 'p-1', name: 'S-Huset', url: 'https://example.com/shuset', active: true },
        ]);

        const [firstCallUrl] = mockFetch.mock.calls[0] as [string];
        const url = new URL(firstCallUrl);
        expect(url.pathname).toBe('/api/pages');
        expect(url.searchParams.get('page')).toBe('0');
        expect(url.searchParams.get('size')).toBe('100');
    });

    it('returns an empty page list when backend has no pages', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 100,
            hasNext: false,
            hasPrevious: false,
        }));

        await expect(getPages()).resolves.toEqual([]);
    });

    it('keeps working even if some page fields are missing', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            content: [{ id: 'p-2' }],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 100,
            hasNext: false,
            hasPrevious: false,
        }));

        await expect(getPages()).resolves.toEqual([
            { id: 'p-2', name: undefined, url: undefined, active: undefined },
        ]);
    });

    it('passes through network errors when loading pages', async () => {
        const readError = new Error('pages read failed');
        mockFetch.mockRejectedValueOnce(readError);

        await expect(getPages()).rejects.toBe(readError);
    });

    it('maps fetched events into app event format', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            content: [
                {
                    id: 'e-1',
                    pageId: 'p-1',
                    title: 'Friday Bar',
                    description: 'Live music',
                    startTime: '2026-02-10T17:00:00.000Z',
                    endTime: '2026-02-10T22:00:00.000Z',
                    place: { name: 'DTU' },
                    coverImageId: 42,
                    eventUrl: 'https://example.com/event/e-1',
                    createdAt: '2026-01-01T10:00:00.000Z',
                    updatedAt: '2026-01-02T10:00:00.000Z',
                },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 100,
            hasNext: false,
            hasPrevious: false,
        }));

        const events = await getEvents();

        expect(events).toEqual([
            {
                id: 'e-1',
                pageId: 'p-1',
                title: 'Friday Bar',
                description: 'Live music',
                startTime: '2026-02-10T17:00:00.000Z',
                endTime: '2026-02-10T22:00:00.000Z',
                place: { name: 'DTU' },
                coverImageUrl: expect.stringMatching(/\/media\/42$/),
                eventURL: 'https://example.com/event/e-1',
                createdAt: '2026-01-01T10:00:00.000Z',
                updatedAt: '2026-01-02T10:00:00.000Z',
            },
        ]);

        const [firstCallUrl] = mockFetch.mock.calls[0] as [string];
        const url = new URL(firstCallUrl);
        expect(url.pathname).toBe('/api/events');
        expect(url.searchParams.get('page')).toBe('0');
        expect(url.searchParams.get('size')).toBe('100');
        expect(url.searchParams.get('sort')).toBe('startTime,asc');
    });

    it('returns an empty event list when backend has no events', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 100,
            hasNext: false,
            hasPrevious: false,
        }));

        await expect(getEvents()).resolves.toEqual([]);
    });

    it('keeps working even if some event fields are missing', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            content: [
                {
                    id: 'e-missing',
                    pageId: 'p-1',
                    title: 'Event Without Extras',
                    startTime: '2026-03-01T12:00:00.000Z',
                    createdAt: '2026-01-01T00:00:00.000Z',
                    updatedAt: '2026-01-01T00:00:00.000Z',
                },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 100,
            hasNext: false,
            hasPrevious: false,
        }));

        await expect(getEvents()).resolves.toEqual([
            {
                id: 'e-missing',
                pageId: 'p-1',
                title: 'Event Without Extras',
                description: undefined,
                startTime: '2026-03-01T12:00:00.000Z',
                endTime: undefined,
                place: undefined,
                coverImageUrl: undefined,
                eventURL: undefined,
                createdAt: '2026-01-01T00:00:00.000Z',
                updatedAt: '2026-01-01T00:00:00.000Z',
            },
        ]);
    });

    it('throws a fetch status error when loading events fails', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({ message: 'boom' }, 500, 'Internal Server Error'));

        await expect(getEvents()).rejects.toThrow('Failed to fetch events: 500 Internal Server Error - boom');
    });

    it('returns null when event does not exist', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({}, 404, 'Not Found'));

        await expect(getEventById('missing')).resolves.toBeNull();
    });

    it('passes through network errors when loading one event', async () => {
        const readError = new Error('single event read failed');
        mockFetch.mockRejectedValueOnce(readError);

        await expect(getEventById('fail')).rejects.toBe(readError);
    });

    it('maps fetched event to app event format', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse({
            id: 'evt-1',
            pageId: 'page-1',
            title: 'Sample Event',
            description: 'Desc',
            startTime: '2026-01-01T12:00:00.000Z',
            endTime: '2026-01-01T14:00:00.000Z',
            place: { name: 'DTU' },
            coverImageId: 8,
            eventUrl: 'https://example.com/event',
            createdAt: '2025-12-01T10:00:00.000Z',
            updatedAt: '2025-12-02T10:00:00.000Z',
        }));

        await expect(getEventById('evt-1')).resolves.toEqual({
            id: 'evt-1',
            pageId: 'page-1',
            title: 'Sample Event',
            description: 'Desc',
            startTime: '2026-01-01T12:00:00.000Z',
            endTime: '2026-01-01T14:00:00.000Z',
            place: { name: 'DTU' },
            coverImageUrl: expect.stringMatching(/\/media\/8$/),
            eventURL: 'https://example.com/event',
            createdAt: '2025-12-01T10:00:00.000Z',
            updatedAt: '2025-12-02T10:00:00.000Z',
        });
    });
});