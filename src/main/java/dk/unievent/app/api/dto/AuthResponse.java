package dk.unievent.app.api.dto;

public record AuthResponse(
	String token,
	String refreshToken,
	String username,
	String email,
	long accessTokenExpiresInMs,
	long refreshTokenExpiresInMs,
	String csrfToken
) {}