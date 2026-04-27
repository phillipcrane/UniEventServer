import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getFacebookAuthUrl } from '../../services/facebook';

// Mock the auth module so getAuthToken returns a controlled value without
// touching localStorage. vi.mock is hoisted by Vitest before any imports,
// so the mocked version is in place when facebook.ts is first evaluated.
vi.mock('../../services/auth', () => ({
    getAuthToken: vi.fn(() => 'my-jwt'),
}));

const mockFetch = vi.fn();

beforeEach(() => {
    mockFetch.mockReset();
    vi.stubGlobal('fetch', mockFetch);
});

afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
});

function jsonResponse(body: unknown, status = 200) {
    return Promise.resolve({
        ok: status >= 200 && status < 300,
        status,
        json: () => Promise.resolve(body),
    });
}

describe('getFacebookAuthUrl', () => {
    it('calls /api/facebook/auth with Bearer token and returns url', async () => {
        const expectedUrl = 'https://www.facebook.com/dialog/oauth?client_id=123&state=abc';
        mockFetch.mockReturnValueOnce(jsonResponse({ url: expectedUrl, state: 'abc' }));

        const result = await getFacebookAuthUrl();

        expect(mockFetch).toHaveBeenCalledWith(
            expect.stringContaining('/api/facebook/auth'),
            expect.objectContaining({ headers: { Authorization: 'Bearer my-jwt' } })
        );
        expect(result).toBe(expectedUrl);
    });

    it('hits the /api/facebook/auth endpoint', async () => {
        mockFetch.mockReturnValueOnce(jsonResponse({ url: 'https://fb.com/auth', state: 'x' }));

        await getFacebookAuthUrl();

        const calledUrl: string = mockFetch.mock.calls[0][0] as string;
        expect(calledUrl).toMatch(/\/api\/facebook\/auth$/);
    });

    it('throws when the backend returns a non-ok response', async () => {
        mockFetch.mockReturnValueOnce(jsonResponse({ message: 'Unauthorized' }, 401));

        await expect(getFacebookAuthUrl()).rejects.toThrow('Unauthorized');
    });

    it('throws a generic message when backend error has no message field', async () => {
        mockFetch.mockReturnValueOnce(jsonResponse({}, 500));

        await expect(getFacebookAuthUrl()).rejects.toThrow('Failed to get Facebook auth URL');
    });
});
