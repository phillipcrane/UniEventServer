import { getCsrfToken } from './auth';

const STATE_CHANGING_METHODS = new Set(['POST', 'PUT', 'DELETE', 'PATCH']);

const isStateChangingMethod = (method?: string): boolean => {
    if (!method) return false;
    return STATE_CHANGING_METHODS.has(method.toUpperCase());
};

export async function apiCall(url: string, options: RequestInit = {}): Promise<Response> {
    const headers = new Headers(options.headers || {});

    if (!headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    if (isStateChangingMethod(options.method)) {
        const csrfToken = getCsrfToken();
        if (!csrfToken) {
            throw new Error('CSRF token not available. Please login again.');
        }
        headers.set('X-CSRF-Token', csrfToken);
    }

    try {
        return await fetch(url, {
            ...options,
            credentials: 'include',
            headers,
        });
    } catch (error) {
        const message = error instanceof Error ? error.message : 'Network request failed.';
        throw new Error(message);
    }
}
