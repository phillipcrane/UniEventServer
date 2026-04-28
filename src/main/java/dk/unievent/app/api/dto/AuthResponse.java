package dk.unievent.app.api.dto;

public record AuthResponse(
	String username,
	String email,
	java.util.List<String> roles,
	String csrfToken
) {}