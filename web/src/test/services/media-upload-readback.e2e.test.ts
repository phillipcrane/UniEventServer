import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getEventById, uploadEventCover } from '../../services/dal';
import { resetCsrfTokenForTesting, setCsrfToken } from '../../services/csrf';

type CapturedRequest = {
    path: string;
    method: string;
    csrfHeader: string | null;
    credentials: RequestCredentials | undefined;
    body: BodyInit | null | undefined;
};

const mockFetch = vi.fn<typeof fetch>();

function eventResponse(coverImageId?: number) {
    return {
        id: 'event-1',
        pageId: 'page-1',
        title: 'Robotics Night',
        description: 'Build robots',
        startTime: '2026-06-01T10:30:00.000Z',
        endTime: '2026-06-01T12:30:00.000Z',
        place: { name: 'Oticon Hall' },
        coverImageId,
        eventUrl: 'https://example.com/events/event-1',
        createdAt: '2026-05-01T00:00:00.000Z',
        updatedAt: '2026-05-01T00:00:00.000Z',
    };
}

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

function readUrl(input: RequestInfo | URL): string {
    if (typeof input === 'string') {
        return input;
    }
    if (input instanceof URL) {
        return input.toString();
    }
    return input.url;
}

function installFakeMediaServer() {
    let storedCoverImageId: number | undefined;
    const requests: CapturedRequest[] = [];

    mockFetch.mockImplementation(async (input: RequestInfo | URL, init: RequestInit = {}) => {
        const url = new URL(readUrl(input), window.location.origin);
        const method = (init.method ?? 'GET').toUpperCase();
        const headers = new Headers(init.headers);

        requests.push({
            path: url.pathname,
            method,
            csrfHeader: headers.get('X-CSRF-Token'),
            credentials: init.credentials,
            body: init.body,
        });

        if (url.pathname === '/api/events/event-1/coverImage' && method === 'POST') {
            if (headers.get('X-CSRF-Token') !== 'csrf-media') {
                return jsonResponse({ message: 'CSRF token validation failed' }, 403);
            }
            if (!(init.body instanceof FormData)) {
                return jsonResponse({ message: 'Expected multipart form data' }, 400);
            }

            storedCoverImageId = 77;
            return jsonResponse(eventResponse(storedCoverImageId));
        }

        if (url.pathname === '/api/events/event-1' && method === 'GET') {
            return jsonResponse(eventResponse(storedCoverImageId));
        }

        return jsonResponse({ message: `Unexpected request: ${method} ${url.pathname}` }, 500);
    });

    return { requests };
}

describe('media upload and event readback service flow', () => {
    beforeEach(() => {
        resetCsrfTokenForTesting();
        mockFetch.mockReset();
        vi.stubGlobal('fetch', mockFetch);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('uploads an event cover image and reads back the media URL on the event', async () => {
        const server = installFakeMediaServer();
        const file = new File(['image-bytes'], 'cover.png', { type: 'image/png' });
        setCsrfToken('csrf-media');

        const uploaded = await uploadEventCover('event-1', file);
        const loaded = await getEventById('event-1');

        expect(uploaded.coverImageUrl).toEqual(expect.stringMatching(/\/media\/77$/));
        expect(loaded).toMatchObject({
            id: 'event-1',
            title: 'Robotics Night',
            coverImageUrl: expect.stringMatching(/\/media\/77$/),
        });

        expect(server.requests.map((request) => `${request.method} ${request.path}`)).toEqual([
            'POST /api/events/event-1/coverImage',
            'GET /api/events/event-1',
        ]);
        expect(server.requests[0]).toMatchObject({
            csrfHeader: 'csrf-media',
            credentials: 'include',
        });
        expect(server.requests[0].body).toBeInstanceOf(FormData);
    });
});
