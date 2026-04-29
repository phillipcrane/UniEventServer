import { getCsrfToken } from './csrf';

const STATE_CHANGING_METHODS = new Set(['POST', 'PUT', 'DELETE', 'PATCH']);
let refreshPromise: Promise<boolean> | null = null;

export type ApiCallOptions = RequestInit & {
    skipAuthErrorHandling?: boolean;
    retryOnUnauthorized?: boolean;
};

const isStateChangingMethod = (method?: string): boolean => {
    if (!method) return false;
    return STATE_CHANGING_METHODS.has(method.toUpperCase());
};

export async function apiCall(url: string, options: ApiCallOptions = {}): Promise<Response> {
    const { skipAuthErrorHandling = false, retryOnUnauthorized = true, ...requestOptions } = options;
    const response = await sendRequest(url, requestOptions);

    if (skipAuthErrorHandling) {
        return response;
    }

    if (response.status === 401 && retryOnUnauthorized) {
        const refreshed = await ensureSessionRefreshed();
        if (refreshed) {
            return sendRequest(url, requestOptions);
        }
        await clearAuthAndThrow(401, 'Session expired. Please login again.');
    }

    if (response.status === 403) {
        const message = await readErrorMessage(response);
        const normalizedMessage = message.toLowerCase();
        if (normalizedMessage.includes('csrf') || normalizedMessage.includes('compromised')) {
            await clearAuthAndThrow(403, message);
        }
    }

    return response;
}

async function sendRequest(url: string, options: RequestInit = {}): Promise<Response> {
    const headers = new Headers(options.headers || {});

    if (!headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    if (isStateChangingMethod(options.method)) {
        const csrfToken = getCsrfToken();
        if (csrfToken) {
            headers.set('X-CSRF-Token', csrfToken);
        }
    }

    try {
        return await fetch(url, {
            ...options,
            credentials: 'include',
            headers,
        });
    } catch (error) {
        if (error instanceof Error) {
            throw error;
        }
        throw new Error('Network request failed.');
    }
}

async function tryRefreshSession(): Promise<boolean> {
    const { refreshSession } = await import('./auth');
    return refreshSession();
}

async function ensureSessionRefreshed(): Promise<boolean> {
    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise = tryRefreshSession();
    try {
        return await refreshPromise;
    } finally {
        refreshPromise = null;
    }
}

async function clearAuthAndThrow(status: number, message: string): Promise<never> {
    const { clearSessionAndRedirect } = await import('./auth');
    clearSessionAndRedirect();
    throw Object.assign(new Error(message), { status });
}

async function readErrorMessage(response: Response): Promise<string> {
    try {
        const body = await response.clone().json() as { message?: unknown };
        if (typeof body.message === 'string' && body.message.trim() !== '') {
            return body.message;
        }
    } catch {
        // Fall through to status text.
    }
    return response.statusText || 'Request failed.';
}
