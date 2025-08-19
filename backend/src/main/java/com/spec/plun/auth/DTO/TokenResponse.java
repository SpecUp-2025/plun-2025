package com.spec.plun.auth.DTO;

public record TokenResponse (
	String tokenType,
	String accessToken,
	long expiresIn

) {}
