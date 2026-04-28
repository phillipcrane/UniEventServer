package dk.unievent.app.api.dto;

public record AuthResponse(
	String username,
	String email,
	String role,
	java.util.List<String> roles,
	String csrfToken,
	// Included for non-browser clients (CLI, API tools) that use Bearer tokens.
	// Browser clients should use the HttpOnly cookie set in Set-Cookie response headers instead.
	String token
) {}