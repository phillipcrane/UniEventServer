import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const mockFetch = vi.fn();
const mockRefreshSession = vi.fn();
const mockClearSessionAndRedirect = vi.fn();
const mockGetCsrfToken = vi.fn();

vi.mock('../../services/csrf', () => ({
    getCsrfToken: () => mockGetCsrfToken(),
}));

vi.mock('../../services/auth', () => ({
    refreshSession: () => mockRefreshSession(),
    clearSessionAndRedirect: () => mockClearSessionAndRedirect(),
}));

import { apiCall } from '../../services/http';

function ok(body: unknown = {}, status = 200): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

describe('http - apiCall', () => {
    beforeEach(() => {
        mockFetch.mockReset();
        mockRefreshSession.mockReset();
        mockClearSessionAndRedirect.mockReset();
        mockGetCsrfToken.mockReset();
        mockGetCsrfToken.mockReturnValue('');
        vi.stubGlobal('fetch', mockFetch);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    // ── sendRequest basics ───────────────────────────────────────────────────

    it('sets Content-Type: application/json by default', async () => {
        mockFetch.mockResolvedValueOnce(ok());

        await apiCall('/api/test');

        const [, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect((init.headers as Headers).get('Content-Type')).toBe('application/json');
    });

    it('does not override an existing Content-Type header', async () => {
        mockFetch.mockResolvedValueOnce(ok());

        await apiCall('/api/test', { headers: { 'Content-Type': 'text/plain' } });

        const [, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect((init.headers as Headers).get('Content-Type')).toBe('text/plain');
    });

    it('always passes credentials: include', async () => {
        mockFetch.mockResolvedValueOnce(ok());

        await apiCall('/api/test');

        const [, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect((init as RequestInit & { credentials: string }).credentials).toBe('include');
    });

    it('adds X-CSRF-Token header for POST when token is present', async () => {
        mockGetCsrfToken.mockReturnValue('csrf-abc');
        mockFetch.mockResolvedValueOnce(ok());

        await apiCall('/api/test', { method: 'POST' });

        const [, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect((init.headers as Headers).get('X-CSRF-Token')).toBe('csrf-abc');
    });

    it('adds X-CSRF-Token for PUT, DELETE, PATCH', async () => {
        mockGetCsrfToken.mockReturnValue('csrf-xyz');

        for (const method of ['PUT', 'DELETE', 'PATCH']) {
            mockFetch.mockResolvedValueOnce(ok());
            await apiCall('/api/test', { method });
            const [, init] = mockFetch.mock.lastCall as [string, RequestInit];
            expect((init.headers as Headers).get('X-CSRF-Token')).toBe('csrf-xyz');
        }
    });

    it('does not add X-CSRF-Token for GET', async () => {
        mockGetCsrfToken.mockReturnValue('csrf-abc');
        mockFetch.mockResolvedValueOnce(ok());

        await apiCall('/api/test', { method: 'GET' });

        const [, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect((init.headers as Headers).get('X-CSRF-Token')).toBeNull();
    });

    it('does not add X-CSRF-Token when csrf token is empty', async () => {
        mockGetCsrfToken.mockReturnValue('');
        mockFetch.mockResolvedValueOnce(ok());

        await apiCall('/api/test', { method: 'POST' });

        const [, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect((init.headers as Headers).get('X-CSRF-Token')).toBeNull();
    });

    it('retries the request after a successful session refresh on 401', async () => {
        mockRefreshSession.mockResolvedValueOnce(true);
        mockFetch
            .mockResolvedValueOnce(ok({}, 401))
            .mockResolvedValueOnce(ok({ data: 'retried' }));

        const response = await apiCall('/api/test');

        expect(response.status).toBe(200);
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('clears auth and throws on 401 when refresh fails', async () => {
        mockRefreshSession.mockResolvedValueOnce(false);
        mockFetch.mockResolvedValueOnce(ok({}, 401));

        await expect(apiCall('/api/test')).rejects.toMatchObject({ status: 401 });
        expect(mockClearSessionAndRedirect).toHaveBeenCalled();
    });

    it('does not retry when retryOnUnauthorized is false', async () => {
        mockFetch.mockResolvedValueOnce(ok({}, 401));

        const response = await apiCall('/api/test', { retryOnUnauthorized: false });

        expect(response.status).toBe(401);
        expect(mockRefreshSession).not.toHaveBeenCalled();
    });

    it('returns 401 response without retry when skipAuthErrorHandling is true', async () => {
        mockFetch.mockResolvedValueOnce(ok({}, 401));

        const response = await apiCall('/api/test', { skipAuthErrorHandling: true });

        expect(response.status).toBe(401);
        expect(mockRefreshSession).not.toHaveBeenCalled();
    });

    it('clears auth and throws on 403 with a CSRF error message', async () => {
        mockFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ message: 'CSRF token validation failed' }), {
                status: 403,
                headers: { 'Content-Type': 'application/json' },
            })
        );

        await expect(apiCall('/api/test', { method: 'POST' })).rejects.toMatchObject({ status: 403 });
        expect(mockClearSessionAndRedirect).toHaveBeenCalled();
    });

    it('clears auth and throws on 403 with a compromised token message', async () => {
        mockFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ message: 'Refresh token family compromised' }), {
                status: 403,
                headers: { 'Content-Type': 'application/json' },
            })
        );

        await expect(apiCall('/api/test', { method: 'POST' })).rejects.toMatchObject({ status: 403 });
        expect(mockClearSessionAndRedirect).toHaveBeenCalled();
    });

    it('passes through 403 without clearing auth when message is unrelated', async () => {
        mockFetch.mockResolvedValueOnce(
            new Response(JSON.stringify({ message: 'Access denied - insufficient role' }), {
                status: 403,
                headers: { 'Content-Type': 'application/json' },
            })
        );

        const response = await apiCall('/api/test', { method: 'GET' });

        expect(response.status).toBe(403);
        expect(mockClearSessionAndRedirect).not.toHaveBeenCalled();
    });
});
