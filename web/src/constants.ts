export const REFRESH_INTERVAL_MS = 4 * 60 * 1000;
export const REFRESH_THRESHOLD_MS = 5 * 60 * 1000;

export const CSRF_COOKIE_NAME = 'csrf_token';

export const BACKEND_URL = import.meta.env.VITE_BACKEND_URL ?? '';

export const API_AUTH_LOGIN = '/api/auth/login';
export const API_AUTH_CSRF_TOKEN = '/api/auth/csrf-token';
export const API_AUTH_REGISTER = '/api/auth/register';
export const API_AUTH_REGISTER_WITH_KEY = '/api/auth/register-with-key';
export const API_AUTH_ORGANIZER_KEY_VERIFY = '/api/auth/organizer-key/verify';
export const API_AUTH_REFRESH = '/api/auth/refresh';
export const API_AUTH_LOGOUT = '/api/auth/logout';
export const API_AUTH_PROFILE = '/api/auth/profile';

export const DEBOUNCE_MS = 250;
export const SHARE_FEEDBACK_MS = 1400;
export const SAVE_FEEDBACK_MS = 1500;
export const PASSWORD_MIN_LENGTH = 12;
export const ORGANIZER_SIGNUP_SUCCESS_REDIRECT_MS = 1500;

export const NEW_EVENT_THRESHOLD_DAYS = 7;

export const ORGANIZER_REQUEST_FORM_URL = 'https://docs.google.com/forms/d/e/1FAIpQLSdBFOWq8W5qGeeZWpOO8VC8iUpe2mDlao7htn9WgeN6CZXzZg/viewform?usp=dialog';

export const FACEBOOK_APP_VERSION = 'v25.0';
export const FACEBOOK_SCOPES = 'pages_show_list,pages_read_engagement';
