// isValidEmail uses a simple structural check: something@something.something.
// A full RFC-5322 regex would be hundreds of characters long and still not perfect;
// this is intentionally lightweight because the backend re-validates anyway.
export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}
