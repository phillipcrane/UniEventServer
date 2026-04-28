import { BACKEND_URL, API_AUTH_REGISTER, API_AUTH_REGISTER_WITH_KEY } from '../constants';
import { buildUserFromResponse, setCsrfToken, setCurrentUser, storeTokenExpiry, notifyListeners, resolveAccountRole } from '../services/auth';
import { createHttpError } from '../utils/authUtils';
import type { User, AuthApiResponse, SignupRequest } from '../types';

export async function signupWithEmail({ username, email, password, role, organizerKey }: SignupRequest): Promise<User> {
    // Organizer registration uses a dedicated endpoint that validates the key server-side
    // and assigns the organizer role; regular users go through the standard register endpoint.
    const endpoint = organizerKey ? API_AUTH_REGISTER_WITH_KEY : API_AUTH_REGISTER;
    const body = organizerKey
        ? { username, email, password, organizerKey }
        : { username, email, password };

    const response = await fetch(`${BACKEND_URL}${endpoint}`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    if (!response.ok) {
        const errBody = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw createHttpError(
            response.status,
            (errBody['message'] as string | undefined) ?? response.statusText,
        );
    }

    const data = await response.json() as AuthApiResponse;
    setCsrfToken(data.csrfToken);
    storeTokenExpiry(data.accessTokenExpiresInMs);
    const user: User = {
        ...buildUserFromResponse(data),
        role: resolveAccountRole(data.roles?.[0], undefined) ?? role,
    };
    setCurrentUser(user);
    notifyListeners(user);
    return user;
}
