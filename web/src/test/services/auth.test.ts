import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const mockFetch = vi.fn();

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

function errorResponse(body: unknown, status: number): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

function authResponse(overrides: Partial<{ username: string; email: string; roles: string[]; csrfToken: string; accessTokenExpiresInMs: number }> = {}): unknown {
    return {
        username: 'alice',
        email: 'alice@example.com',
        roles: ['ROLE_USER'],
        csrfToken: 'csrf-token',
        accessTokenExpiresInMs: 3600000,
        ...overrides,
    };
}

import { _resetForTesting, getCurrentUser, loginWithEmail, logout, onUserChanged, signupWithEmail, setCsrfToken } from '../../services/auth';
import { mapAuthError } from '../../utils/authUtils';

describe('auth service', () => {
    beforeEach(() => {
        _resetForTesting();
        // Pre-seed a CSRF token so ensureCsrfToken() returns early without
        // making a fetch call. Unit tests here verify login/signup behaviour,
        // not the CSRF bootstrap flow.
        setCsrfToken('test-csrf');
        mockFetch.mockReset();
        vi.stubGlobal('fetch', mockFetch);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    // ── loginWithEmail ──────────────────────────────────────────────────────

    it('logs in and stores user metadata in memory', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'alice', email: 'alice@example.com' })));

        const user = await loginWithEmail('alice@example.com', 'secret123');

        expect(user).toMatchObject({ username: 'alice', email: 'alice@example.com', role: 'user' });
        expect(getCurrentUser()).toMatchObject({ username: 'alice', email: 'alice@example.com' });
    });

    it('sends credentials and includes credentials:include', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'u', email: 'u@x.com' })));

        await loginWithEmail('u@x.com', 'pass');

        const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect(url).toContain('/api/auth/login');
        expect((init as RequestInit & { credentials: string }).credentials).toBe('include');
        expect(JSON.parse(init.body as string)).toEqual({ email: 'u@x.com', password: 'pass' });
    });

    it('throws with status when login credentials are wrong', async () => {
        mockFetch.mockResolvedValueOnce(errorResponse({ message: 'Bad credentials' }, 401));

        await expect(loginWithEmail('u@x.com', 'wrong')).rejects.toMatchObject({ status: 401 });
    });

    // ── signupWithEmail ─────────────────────────────────────────────────────

    it('registers and stores user metadata in memory', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'bob', email: 'bob@example.com' })));

        const user = await signupWithEmail({ username: 'bob', email: 'bob@example.com', password: 'secret123' });

        expect(user).toMatchObject({ username: 'bob', email: 'bob@example.com', role: 'user' });
        expect(getCurrentUser()).toMatchObject({ username: 'bob' });
    });

    it('sends signup payload to the correct endpoint', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'bob', email: 'bob@x.com' })));

        await signupWithEmail({ username: 'bob', email: 'bob@x.com', password: 'secret123' });

        const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit];
        expect(url).toContain('/api/auth/register');
        expect(JSON.parse(init.body as string)).toEqual({ username: 'bob', email: 'bob@x.com', password: 'secret123' });
    });

    it('throws with status when email is already taken', async () => {
        mockFetch.mockResolvedValueOnce(errorResponse({ message: 'Email is already registered.' }, 409));

        await expect(signupWithEmail({ username: 'bob', email: 'dup@x.com', password: 'secret' })).rejects.toMatchObject({ status: 409 });
    });

    // ── getCurrentUser ──────────────────────────────────────────────────────

    it('returns null when no session exists', () => {
        expect(getCurrentUser()).toBeNull();
    });

    it('returns the user after login', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'carol', email: 'carol@x.com' })));
        await loginWithEmail('carol@x.com', 'pw');

        expect(getCurrentUser()).toMatchObject({ username: 'carol', email: 'carol@x.com', role: 'user' });
    });

    // ── onUserChanged ────────────────────────────────────────────────────────

    it('fires immediately with current user when already logged in', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'dave', email: 'dave@x.com' })));
        await loginWithEmail('dave@x.com', 'pw');

        const callback = vi.fn();
        const unsubscribe = onUserChanged(callback);

        expect(callback).toHaveBeenCalledOnce();
        expect(callback.mock.calls[0][0]).toMatchObject({ username: 'dave', email: 'dave@x.com' });
        unsubscribe();
    });

    it('fires immediately with null when not logged in', () => {
        const callback = vi.fn();
        const unsubscribe = onUserChanged(callback);

        expect(callback).toHaveBeenCalledWith(null);
        unsubscribe();
    });

    it('notifies listeners on login', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'eve', email: 'eve@x.com' })));

        const callback = vi.fn();
        const unsubscribe = onUserChanged(callback);
        callback.mockClear();

        await loginWithEmail('eve@x.com', 'pw');

        expect(callback).toHaveBeenCalledWith(expect.objectContaining({ username: 'eve', email: 'eve@x.com' }));
        unsubscribe();
    });

    it('returns a working unsubscribe function', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'u', email: 'u@x.com' })));

        const callback = vi.fn();
        const unsubscribe = onUserChanged(callback);
        unsubscribe();
        callback.mockClear();

        await loginWithEmail('u@x.com', 'pw');

        expect(callback).not.toHaveBeenCalled();
    });

    // ── logout ───────────────────────────────────────────────────

    it('calls logout endpoint, clears user state, and notifies listeners', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'u', email: 'u@x.com' })));
        await loginWithEmail('u@x.com', 'pw');

        const callback = vi.fn();
        const unsubscribe = onUserChanged(callback);
        callback.mockClear();

        mockFetch.mockResolvedValueOnce(new Response(null, { status: 204 }));
        await logout();

        const [url] = mockFetch.mock.lastCall as [string, RequestInit];
        expect(url).toContain('/api/auth/logout');
        expect(getCurrentUser()).toBeNull();
        expect(callback).toHaveBeenCalledWith(null);
        unsubscribe();
    });

    it('clears local state even when logout endpoint fails', async () => {
        mockFetch.mockResolvedValueOnce(jsonResponse(authResponse({ username: 'u', email: 'u@x.com' })));
        await loginWithEmail('u@x.com', 'pw');

        mockFetch.mockRejectedValueOnce(new Error('network error'));
        await logout();

        expect(getCurrentUser()).toBeNull();
    });

    it('returns "Invalid email or password" for 401', () => {
        expect(mapAuthError({ status: 401 })).toBe('Invalid email or password.');
    });

    it('returns "Invalid email or password" for 403', () => {
        expect(mapAuthError({ status: 403 })).toBe('Invalid email or password.');
    });

    it('returns the server message for 409 conflicts', () => {
        expect(mapAuthError({ status: 409, message: 'Email is already registered.' })).toBe('Email is already registered.');
    });

    it('returns the server message for 400 bad input', () => {
        expect(mapAuthError({ status: 400, message: 'Validation failed.' })).toBe('Validation failed.');
    });

    it('returns a fallback message for unknown errors', () => {
        expect(mapAuthError(new Error('random failure'))).toBe('Something went wrong. Please try again.');
    });

    it('returns a fallback message for null/undefined', () => {
        expect(mapAuthError(null)).toBe('Something went wrong. Please try again.');
    });
});
