package com.spec.plun.auth.DTO;

public record TokenResponse (
	String accessToken,
	String refreshToken
) {}