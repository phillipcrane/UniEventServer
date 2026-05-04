/**
 * Security utilities for organizer registration flow
 * Includes XSS prevention, HTTPS enforcement, and rate limiting
 */

/**
 * Sanitize error messages to prevent XSS attacks
 * Escapes all HTML by using textContent instead of innerHTML
 * @param message - Raw error message from API or internal error
 * @returns Sanitized message safe to display in DOM
 */
export function sanitizeErrorMessage(message: string): string {
  if (!message || typeof message !== 'string') {
    return 'Something went wrong. Please try again.';
  }

  if (typeof document !== 'undefined') {
    const div = document.createElement('div');
    div.textContent = message;
    return div.innerHTML;
  }

  return message
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * Enforce HTTPS for backend communication
 * Throws error if BACKEND_URL uses HTTP (non-HTTPS)
 * Call this at module initialization
 */
export function enforceHttpsBackend(backendUrl: string): void {
  if (!backendUrl) {
    throw new Error('BACKEND_URL environment variable is not configured');
  }

  if (!backendUrl.startsWith('https://')) {
    throw new Error(
      `Backend URL must use HTTPS for security. Current: ${backendUrl}. ` +
      'HTTP is not allowed for authentication and sensitive data transmission.'
    );
  }
}

/**
 * Check if should apply rate limiting based on attempts and timestamp
 * @param attempts - Number of failed attempts
 * @param lastAttemptTime - Timestamp of last attempt (milliseconds)
 * @param maxAttempts - Maximum allowed attempts before lockout (default 5)
 * @param lockoutDuration - Duration of lockout in milliseconds (default 60000 = 1 minute)
 * @returns true if rate limited, false otherwise
 */
export function isRateLimited(
  attempts: number,
  lastAttemptTime: number | null,
  maxAttempts = 5,
  lockoutDuration = 60000
): boolean {
  if (attempts < maxAttempts) {
    return false;
  }

  if (!lastAttemptTime) {
    return false;
  }

  const timeSinceLastAttempt = Date.now() - lastAttemptTime;
  return timeSinceLastAttempt < lockoutDuration;
}

/**
 * Get time remaining until rate limit expires
 * @param lastAttemptTime - Timestamp of last attempt (milliseconds)
 * @param lockoutDuration - Duration of lockout in milliseconds (default 60000)
 * @returns Seconds remaining, or 0 if not rate limited
 */
export function getSecondsUntilRateLimitExpires(
  lastAttemptTime: number | null,
  lockoutDuration = 60000
): number {
  if (!lastAttemptTime) {
    return 0;
  }

  const timeSinceLastAttempt = Date.now() - lastAttemptTime;
  const remaining = Math.ceil((lockoutDuration - timeSinceLastAttempt) / 1000);
  return Math.max(0, remaining);
}

/**
 * Check if confirmation token will expire soon
 * @param tokenExpiresAt - Unix timestamp (seconds) when token expires
 * @param warningThresholdSeconds - Show warning if expires within this many seconds (default 120 = 2 minutes)
 * @returns true if expiring soon, false otherwise
 */
export function isTokenExpiringSoon(
  tokenExpiresAt: number | null,
  warningThresholdSeconds = 120
): boolean {
  if (!tokenExpiresAt) {
    return false;
  }

  const now = Math.floor(Date.now() / 1000);
  return tokenExpiresAt - now < warningThresholdSeconds;
}

/**
 * Get seconds until token expires
 * @param tokenExpiresAt - Unix timestamp (seconds) when token expires
 * @returns Seconds remaining, or 0 if already expired
 */
export function getSecondsUntilTokenExpires(tokenExpiresAt: number | null): number {
  if (!tokenExpiresAt) {
    return 0;
  }

  const now = Math.floor(Date.now() / 1000);
  return Math.max(0, tokenExpiresAt - now);
}

/**
 * Never log sensitive data (for use as reminder in code reviews)
 * This function intentionally does nothing - it's for documentation
 * @param _sensitiveData - DO NOT PASS: passwords, tokens, PII
 */
export function NEVER_LOG_SENSITIVE_DATA(_sensitiveData: unknown): void {
  // This is a guard function to catch accidental logging in code reviews
  // Never actually call this with real sensitive data
}
