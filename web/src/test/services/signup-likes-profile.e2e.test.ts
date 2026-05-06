import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
    _resetForTesting,
    getAccountProfile,
    getCurrentUser,
    getCsrfToken,
    signupWithEmail,
} from '../../services/auth';
import {
    _resetLikesForTesting,
    getLikedEventIdsAsync,
    toggleLikedEvent,
} from '../../services/likes';

type CookieAttributes = {
    value: string;
    httpOnly: boolean;
    secure: boolean;
    sameSite: 'Strict';
    maxAge: number;
};

type CookieJar = Record<string, CookieAttributes | undefined>;

type CapturedRequest = {
    path: string;
    method: string;
    csrfHeader: string | null;
    body: unknown;
    cookiesSeenByServer: Record<string, string>;
};

const mockFetch = vi.fn<typeof fetch>();

function jsonResponse(body: unknown, status = 200, setCookie: string[] = []): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: {
            'Content-Type': 'application/json',
            ...(setCookie.length > 0 ? { 'Set-Cookie': setCookie.join(', ') } : {}),
        },
    });
}

function cookie(value: string, httpOnly: boolean, maxAge: number): CookieAttributes {
    return {
        value,
        httpOnly,
        secure: true,
        sameSite: 'Strict',
        maxAge,
    };
}

function serializeCookie(name: string, attributes: CookieAttributes | undefined): string {
    if (!attributes) {
        throw new Error(`Missing cookie ${name}`);
    }

    return [
        `${name}=${attributes.value}`,
        'Path=/',
        `Max-Age=${attributes.maxAge}`,
        ...(attributes.httpOnly ? ['HttpOnly'] : []),
        attributes.secure ? 'Secure' : '',
        `SameSite=${attributes.sameSite}`,
    ].filter(Boolean).join('; ');
}

function setAuthCookies(cookieJar: CookieJar, access: string, refresh: string, csrf: string): string[] {
    cookieJar.auth_access = cookie(access, true, 86_400);
    cookieJar.auth_refresh = cookie(refresh, true, 604_800);
    cookieJar.csrf_token = cookie(csrf, false, 3_600);

    return [
        serializeCookie('auth_access', cookieJar.auth_access),
        serializeCookie('auth_refresh', cookieJar.auth_refresh),
        serializeCookie('csrf_token', cookieJar.csrf_token),
    ];
}

function visibleCookies(cookieJar: CookieJar, credentials: RequestCredentials | undefined): Record<string, string> {
    if (credentials !== 'include') {
        return {};
    }

    return Object.fromEntries(
        Object.entries(cookieJar)
            .filter((entry): entry is [string, CookieAttributes] => entry[1] !== undefined)
            .map(([name, attributes]) => [name, attributes.value]),
    );
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

function parseBody(init: RequestInit): unknown {
    if (typeof init.body !== 'string' || init.body === '') {
        return undefined;
    }
    return JSON.parse(init.body);
}

function installFakeSignupLikesServer() {
    const cookieJar: CookieJar = {};
    const likedEventIds = new Set<string>();
    const requests: CapturedRequest[] = [];

    mockFetch.mockImplementation(async (input: RequestInfo | URL, init: RequestInit = {}) => {
        const rawUrl = readUrl(input);
        const url = new URL(rawUrl, window.location.origin);
        const path = url.pathname;
        const method = (init.method ?? 'GET').toUpperCase();
        const headers = new Headers(init.headers);
        const requestCookies = visibleCookies(cookieJar, init.credentials);
        const body = parseBody(init);

        requests.push({
            path,
            method,
            csrfHeader: headers.get('X-CSRF-Token'),
            body,
            cookiesSeenByServer: requestCookies,
        });

        if (path === '/api/auth/csrf-token' && method === 'GET') {
            return jsonResponse({ csrfToken: 'bootstrap-csrf' });
        }

        if (path === '/api/auth/register' && method === 'POST') {
            if (headers.get('X-CSRF-Token') !== 'bootstrap-csrf') {
                return jsonResponse({ message: 'CSRF token validation failed' }, 403);
            }

            const setCookie = setAuthCookies(cookieJar, 'access-signup', 'refresh-signup', 'csrf-signup');
            return jsonResponse({
                username: 'alice',
                email: 'alice@example.com',
                roles: ['ROLE_USER'],
                csrfToken: 'csrf-signup',
                accessTokenExpiresInMs: 3_600_000,
            }, 200, setCookie);
        }

        const authenticated = requestCookies.auth_access === 'access-signup';
        if (!authenticated && path.startsWith('/api/users/me/likes')) {
            return jsonResponse({ message: 'Unauthorized' }, 401);
        }

        if (path === '/api/users/me/likes' && method === 'GET') {
            return jsonResponse({ eventIds: Array.from(likedEventIds) });
        }

        if (path === '/api/users/me/likes/event-1' && method === 'PUT') {
            const csrfValid = requestCookies.csrf_token === 'csrf-signup'
                && headers.get('X-CSRF-Token') === 'csrf-signup';
            if (!csrfValid) {
                return jsonResponse({ message: 'CSRF token validation failed' }, 403);
            }

            likedEventIds.add('event-1');
            return jsonResponse({ eventIds: Array.from(likedEventIds) });
        }

        if (path === '/api/auth/profile' && method === 'GET') {
            if (!authenticated) {
                return jsonResponse({ message: 'Unauthorized' }, 401);
            }
            return jsonResponse({ role: 'USER', organizerNames: [] });
        }

        return jsonResponse({ message: `Unexpected request: ${method} ${path}` }, 500);
    });

    return { cookieJar, likedEventIds, requests };
}

describe('signup, likes, and profile service flow', () => {
    beforeEach(() => {
        _resetForTesting();
        _resetLikesForTesting();
        localStorage.clear();
        mockFetch.mockReset();
        vi.stubGlobal('fetch', mockFetch);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('signs up, stores the session, likes an event, and reads profile likes back', async () => {
        const server = installFakeSignupLikesServer();

        const user = await signupWithEmail({
            username: 'alice',
            email: 'alice@example.com',
            password: 'secret123',
        });

        expect(user).toMatchObject({
            uid: 'alice',
            username: 'alice',
            email: 'alice@example.com',
            role: 'user',
        });
        expect(getCurrentUser()).toMatchObject({ email: 'alice@example.com' });
        expect(getCsrfToken()).toBe('csrf-signup');
        expect(server.cookieJar.auth_access).toMatchObject({
            value: 'access-signup',
            httpOnly: true,
            secure: true,
            sameSite: 'Strict',
        });

        expect(await getLikedEventIdsAsync(user.uid)).toEqual([]);
        expect(await toggleLikedEvent(user.uid, 'event-1')).toBe(true);
        expect(await getLikedEventIdsAsync(user.uid)).toEqual(['event-1']);

        const profile = await getAccountProfile(user.uid);

        expect(profile).toEqual({ role: 'user', organizerNames: [] });
        expect(server.likedEventIds).toEqual(new Set(['event-1']));

        expect(server.requests.map((request) => `${request.method} ${request.path}`)).toEqual([
            'GET /api/auth/csrf-token',
            'POST /api/auth/register',
            'GET /api/users/me/likes',
            'PUT /api/users/me/likes/event-1',
            'GET /api/auth/profile',
        ]);

        expect(server.requests[1]).toMatchObject({
            csrfHeader: 'bootstrap-csrf',
            body: {
                username: 'alice',
                email: 'alice@example.com',
                password: 'secret123',
            },
            cookiesSeenByServer: {},
        });
        expect(server.requests[3]).toMatchObject({
            csrfHeader: 'csrf-signup',
            cookiesSeenByServer: {
                auth_access: 'access-signup',
                auth_refresh: 'refresh-signup',
                csrf_token: 'csrf-signup',
            },
        });
    });
});
