import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
    _resetForTesting,
    getCsrfToken,
    loginWithEmail,
    logout,
    refreshSession,
} from '../../services/auth';
import { apiCall } from '../../services/http';

const mockFetch = vi.fn<typeof fetch>();

type AuthResponse = {
    username: string;
    email: string;
    roles: string[];
    csrfToken: string;
    accessTokenExpiresInMs: number;
};

function authResponse(overrides: Partial<AuthResponse> = {}): AuthResponse {
    return {
        username: 'alice',
        email: 'alice@example.com',
        roles: ['ROLE_USER'],
        csrfToken: 'csrf-token',
        accessTokenExpiresInMs: 3_600_000,
        ...overrides,
    };
}

function jsonResponse(body: unknown, status = 200, headers: HeadersInit = {}): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: {
            'Content-Type': 'application/json',
            ...headers,
        },
    });
}

function emptyResponse(status = 204, headers: HeadersInit = {}): Response {
    return new Response(null, { status, headers });
}

function csrfResponse(token: string): Response {
    return jsonResponse({ csrfToken: token });
}

function lastFetchCall(): [string, RequestInit] {
    return mockFetch.mock.lastCall as [string, RequestInit];
}

function headersFrom(init: RequestInit): Headers {
    return new Headers(init.headers);
}

describe('frontend auth integration flow', () => {
    beforeEach(() => {
        _resetForTesting();
        mockFetch.mockReset();
        vi.stubGlobal('fetch', mockFetch);
        document.cookie = 'csrf_token=; Max-Age=0; Path=/';
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    describe('loginWithEmail', () => {
        it('calls the login endpoint with credentials and stores the CSRF token in memory', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({
                username: 'alice',
                email: 'alice@example.com',
                csrfToken: 'login-csrf-token',
            })));

            const user = await loginWithEmail('alice@example.com', 'secret123');

            const [url, init] = lastFetchCall();
            expect(url).toContain('/api/auth/login');
            expect(init).toMatchObject({
                method: 'POST',
                credentials: 'include',
            });
            expect(headersFrom(init).get('Content-Type')).toBe('application/json');
            expect(JSON.parse(init.body as string)).toEqual({
                email: 'alice@example.com',
                password: 'secret123',
            });
            expect(user).toMatchObject({
                username: 'alice',
                email: 'alice@example.com',
                role: 'user',
            });
            expect(getCsrfToken()).toBe('login-csrf-token');
        });
    });

    describe('getCsrfToken', () => {
        it('returns an empty string before login', () => {
            expect(getCsrfToken()).toBe('');
        });

        it('returns the stored token after login', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'stored-csrf-token' })));

            await loginWithEmail('alice@example.com', 'secret123');

            expect(getCsrfToken()).toBe('stored-csrf-token');
        });
    });

    describe('apiCall', () => {
        it('does not include a CSRF header for GET requests and always includes credentials', async () => {
            mockFetch.mockResolvedValueOnce(jsonResponse({ ok: true }));

            await apiCall('/api/events');

            const [, init] = lastFetchCall();
            expect(init.credentials).toBe('include');
            expect(headersFrom(init).has('X-CSRF-Token')).toBe(false);
        });

        it('includes X-CSRF-Token for POST requests after login and always includes credentials', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'post-csrf-token' })));
            await loginWithEmail('alice@example.com', 'secret123');
            mockFetch.mockResolvedValueOnce(jsonResponse({ id: 'event-1' }));

            await apiCall('/api/events', {
                method: 'POST',
                body: JSON.stringify({ title: 'Uni Night' }),
            });

            const [, init] = lastFetchCall();
            const headers = headersFrom(init);
            expect(init.credentials).toBe('include');
            expect(headers.get('X-CSRF-Token')).toBe('post-csrf-token');
            expect(headers.get('Content-Type')).toBe('application/json');
        });

        it('refreshes the session and retries once when an API call returns 401', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'initial-csrf-token' })));
            await loginWithEmail('alice@example.com', 'secret123');
            mockFetch.mockResolvedValueOnce(jsonResponse({ message: 'Access token expired.' }, 401));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'refreshed-csrf-token' })));
            mockFetch.mockResolvedValueOnce(jsonResponse({ id: 'event-1' }, 201));

            const response = await apiCall('/api/events', {
                method: 'POST',
                body: JSON.stringify({ title: 'Uni Night' }),
            });

            expect(response.status).toBe(201);
            expect(mockFetch).toHaveBeenCalledTimes(5);
            const refreshCall = mockFetch.mock.calls[3] as [string, RequestInit];
            const retriedCall = mockFetch.mock.calls[4] as [string, RequestInit];
            expect(refreshCall[0]).toContain('/api/auth/refresh');
            expect(headersFrom(refreshCall[1]).get('X-CSRF-Token')).toBe('initial-csrf-token');
            expect(retriedCall[0]).toBe('/api/events');
            expect(headersFrom(retriedCall[1]).get('X-CSRF-Token')).toBe('refreshed-csrf-token');
            expect(getCsrfToken()).toBe('refreshed-csrf-token');
        });

        it('clears the session when a protected request fails CSRF validation', async () => {
            window.history.pushState({}, '', '/login');
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'csrf-token' })));
            await loginWithEmail('alice@example.com', 'secret123');
            mockFetch.mockResolvedValueOnce(jsonResponse({ message: 'CSRF token validation failed' }, 403));

            await expect(apiCall('/api/events', {
                method: 'POST',
                body: JSON.stringify({ title: 'Uni Night' }),
            })).rejects.toMatchObject({
                status: 403,
                message: 'CSRF token validation failed',
            });

            expect(getCsrfToken()).toBe('');
        });
    });

    describe('logout', () => {
        it('calls the logout endpoint and clears the in-memory CSRF token while the server clears cookies', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'logout-csrf-token' })));
            await loginWithEmail('alice@example.com', 'secret123');
            document.cookie = 'csrf_token=logout-csrf-token; Path=/';

            mockFetch.mockResolvedValueOnce(emptyResponse(204, {
                'Set-Cookie': [
                    'auth_access=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict',
                    'auth_refresh=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict',
                    'csrf_token=; Path=/; Max-Age=0; Secure; SameSite=Strict',
                ].join(', '),
            }));

            await logout();

            const [url, init] = lastFetchCall();
            expect(url).toContain('/api/auth/logout');
            expect(init.method).toBe('POST');
            expect(init.credentials).toBe('include');
            expect(headersFrom(init).get('X-CSRF-Token')).toBe('logout-csrf-token');
            expect(getCsrfToken()).toBe('');
        });
    });

    describe('refreshSession', () => {
        it('updates the CSRF token and returns true on success', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'initial-csrf-token' })));
            await loginWithEmail('alice@example.com', 'secret123');
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({
                username: 'alice',
                email: 'alice@example.com',
                csrfToken: 'refreshed-csrf-token',
            })));

            const refreshed = await refreshSession();

            const [url, init] = lastFetchCall();
            expect(refreshed).toBe(true);
            expect(url).toContain('/api/auth/refresh');
            expect(init.method).toBe('POST');
            expect(init.credentials).toBe('include');
            expect(headersFrom(init).get('X-CSRF-Token')).toBe('initial-csrf-token');
            expect(getCsrfToken()).toBe('refreshed-csrf-token');
        });

        it('returns false on failure and clears state for unauthorized refresh responses', async () => {
            mockFetch.mockResolvedValueOnce(csrfResponse('bootstrap-csrf-token'));
            mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ csrfToken: 'initial-csrf-token' })));
            await loginWithEmail('alice@example.com', 'secret123');
            mockFetch.mockResolvedValueOnce(jsonResponse({ message: 'Unauthorized' }, 401));

            const refreshed = await refreshSession();

            expect(refreshed).toBe(false);
            expect(getCsrfToken()).toBe('');
        });
    });
});
