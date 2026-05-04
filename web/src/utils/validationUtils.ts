/**
 * Validation utilities for organizer key registration flow
 */

/**
 * Validates an organizer key format
 * Must be exactly 32 alphanumeric characters
 */
export function isValidOrganizerKey(key: string): boolean {
  return /^[a-zA-Z0-9]{32}$/.test(key);
}

/**
 * Validates a username format
 * Must be 3-50 characters, alphanumeric with underscores and hyphens
 */
export function isValidUsername(username: string): boolean {
  return /^[a-zA-Z0-9_-]{3,50}$/.test(username);
}

/**
 * Validates a password
 * Must be 12-100 characters
 */
export function isValidPassword(password: string): boolean {
  return password.length >= 12 && password.length <= 100;
}

/**
 * Validates that two passwords match
 */
export function passwordsMatch(password: string, confirmPassword: string): boolean {
  return password === confirmPassword;
}

/**
 * Validates an email format
 */
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/**
 * Validates email length (RFC practical max)
 */
export function isValidEmailLength(email: string): boolean {
  return email.length <= 255;
}
