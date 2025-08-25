package com.spec.plun.email.DTO;

public record VerifyCodeRequest (
	String email,
	String code
){}
