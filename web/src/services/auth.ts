const BACKEND_URL = import.meta.env.VITE_BACKEND_URL ?? '';
const TOKEN_KEY = 'unievent_token';
const USER_KEY = 'unievent_user';

export type AuthUser = {
    username: string;
    email: string;
    token: string;
    uid?: string;
    displayName?: string;
    photoURL?: string | null;
    role?: AccountRole;
    organizerNames?: string[];
};

export type AccountRole = 'user' | 'organizer';

type SignupInput = {
    username: string;
    email: string;
    password: string;
    role?: AccountRole;
    organizerNames?: string[];
};

type AuthErrorContext = 'login' | 'signup' | 'general';

type HttpError = Error & { status: number };

function createHttpError(status: number, message: string): HttpError {
    return Object.assign(new Error(message), { status });
}

// Module-level listener list for auth state subscriptions
const listeners: Array<(user: AuthUser | null) => void> = [];

function notifyListeners(user: AuthUser | null): void {
    listeners.forEach((cb) => cb(user));
}

function normalizeRole(value: unknown): AccountRole | undefined {
    if (typeof value !== 'string') return undefined;
    const normalized = value.trim().toUpperCase();
    if (normalized === 'ORGANIZER' || normalized === 'ROLE_ORGANIZER') return 'organizer';
    if (normalized === 'USER' || normalized === 'ROLE_USER') return 'user';
    return undefined;
}

function resolveAccountRole(
    roleCandidate: unknown,
    organizerNamesCandidate: unknown,
    fallback: AccountRole = 'user',
): AccountRole {
    const normalizedRole = normalizeRole(roleCandidate);
    if (normalizedRole) return normalizedRole;

    if (Array.isArray(organizerNamesCandidate) && organizerNamesCandidate.length > 0) {
        return 'organizer';
    }

    return fallback;
}

function getRoleFromJwt(token: string): AccountRole | undefined {
    try {
        const [, payloadBase64] = token.split('.');
        if (!payloadBase64) return undefined;

        const padded = payloadBase64.padEnd(payloadBase64.length + ((4 - (payloadBase64.length % 4)) % 4), '=');
        const payloadJson = atob(padded.replace(/-/g, '+').replace(/_/g, '/'));
        const payload = JSON.parse(payloadJson) as { roles?: unknown; role?: unknown };

        if (Array.isArray(payload.roles)) {
            for (const roleValue of payload.roles) {
                if (normalizeRole(roleValue) === 'organizer') return 'organizer';
            }
            for (const roleValue of payload.roles) {
                if (normalizeRole(roleValue) === 'user') return 'user';
            }
        }

        return normalizeRole(payload.role);
    } catch {
        return undefined;
    }
}

function persistUser(user: AuthUser): void {
    localStorage.setItem(TOKEN_KEY, user.token);
    localStorage.setItem(USER_KEY, JSON.stringify({
        username: user.username,
        email: user.email,
        uid: user.uid,
        displayName: user.displayName,
        photoURL: user.photoURL,
        role: user.role,
        organizerNames: user.organizerNames,
    }));
}

function clearUser(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export function getCurrentUser(): AuthUser | null {
    const token = localStorage.getItem(TOKEN_KEY);
    const raw = localStorage.getItem(USER_KEY);
    if (!token || !raw) return null;
    try {
        const stored = JSON.parse(raw) as {
            username: string;
            email: string;
            uid?: string;
            displayName?: string;
            photoURL?: string | null;
            role?: AccountRole;
            organizerNames?: string[];
        };

        const roleFromToken = getRoleFromJwt(token);

        const organizerNames = Array.isArray(stored.organizerNames) ? stored.organizerNames : undefined;

        return {
            username: stored.username,
            email: stored.email,
            token,
            uid: stored.uid ?? stored.username,
            displayName: stored.displayName ?? stored.username,
            photoURL: stored.photoURL,
            role: resolveAccountRole(roleFromToken ?? stored.role, organizerNames),
            organizerNames,
        };
    } catch {
        return null;
    }
}

export function getAuthToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export async function loginWithEmail(email: string, password: string): Promise<AuthUser> {
    const response = await fetch(`${BACKEND_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw createHttpError(
            response.status,
            (body['message'] as string | undefined) ?? response.statusText,
        );
    }

    const data = await response.json() as { token: string; username: string; email: string };
    const user: AuthUser = {
        token: data.token,
        username: data.username,
        email: data.email,
        uid: data.username,
        displayName: data.username,
        role: getRoleFromJwt(data.token),
    };
    persistUser(user);
    notifyListeners(user);
    return user;
}

export async function signupWithEmail({ username, email, password, role, organizerNames }: SignupInput): Promise<AuthUser> {
    const response = await fetch(`${BACKEND_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password, role, organizerNames }),
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw createHttpError(
            response.status,
            (body['message'] as string | undefined) ?? response.statusText,
        );
    }

    const data = await response.json() as { token: string; username: string; email: string };
    const user: AuthUser = {
        token: data.token,
        username: data.username,
        email: data.email,
        uid: data.username,
        displayName: data.username,
        role: getRoleFromJwt(data.token) ?? role,
        organizerNames: organizerNames ? [...organizerNames] : undefined,
    };
    persistUser(user);
    notifyListeners(user);
    return user;
}

export function onAuthUserChanged(callback: (user: AuthUser | null) => void): () => void {
    listeners.push(callback);
    // fire immediately with current state
    callback(getCurrentUser());
    return () => {
        const idx = listeners.indexOf(callback);
        if (idx !== -1) listeners.splice(idx, 1);
    };
}

export async function signOutCurrentUser(): Promise<void> {
    clearUser();
    notifyListeners(null);
}

export function getStoredAccountRole(uid: string): AccountRole {
    const user = getCurrentUser();
    if (!user || (uid && user.uid !== uid)) {
        return 'user';
    }
    return user.role ?? 'user';
}

export function getStoredOrganizerNames(uid: string): string[] {
    const user = getCurrentUser();
    if (!user || (uid && user.uid !== uid)) {
        return [];
    }
    return Array.isArray(user.organizerNames) ? [...user.organizerNames] : [];
}

export async function getAccountProfile(uid?: string): Promise<{ role: AccountRole; organizerNames: string[] }> {
    const user = getCurrentUser();
    if (!user || !user.token || (uid && user.uid !== uid)) {
        return { role: 'user', organizerNames: [] };
    }

    const fallbackRole = resolveAccountRole(user.role ?? getRoleFromJwt(user.token), user.organizerNames);
    const fallbackOrganizerNames = Array.isArray(user.organizerNames) ? [...user.organizerNames] : [];

    const response = await fetch(`${BACKEND_URL}/api/auth/profile`, {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${user.token}`,
        },
    });

    if (!response.ok) {
        return { role: fallbackRole, organizerNames: fallbackOrganizerNames };
    }

    const data = await response.json() as { role?: string; organizerNames?: string[] };
    const profileOrganizerNames = Array.isArray(data.organizerNames) ? data.organizerNames : fallbackOrganizerNames;
    const profile = {
        role: resolveAccountRole(data.role, profileOrganizerNames, fallbackRole),
        organizerNames: profileOrganizerNames,
    };

    const updatedUser: AuthUser = {
        ...user,
        role: profile.role,
        organizerNames: [...profile.organizerNames],
    };
    persistUser(updatedUser);
    notifyListeners(updatedUser);

    return profile;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function mapAuthError(error: unknown, _context?: AuthErrorContext): string {
    if (error && typeof error === 'object') {
        const e = error as { status?: number; message?: string };
        if (e.status === 401 || e.status === 403) {
            return 'Invalid email or password.';
        }
        if (e.status === 409 || (e.status !== undefined && e.message && e.message.toLowerCase().includes('already'))) {
            return e.message ?? 'Account already exists.';
        }
        if (e.status === 400) {
            return e.message ?? 'Invalid input. Please check your details.';
        }
        // Only surface the message when it came from our backend (has a known status code).
        if (e.status !== undefined && e.message) {
            return e.message;
        }
    }
    return 'Something went wrong. Please try again.';
}
