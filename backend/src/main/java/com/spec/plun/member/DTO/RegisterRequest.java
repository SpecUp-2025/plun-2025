package com.spec.plun.member.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
	@NotBlank
	private String email;
	@NotBlank
	private String password;
	@NotBlank
	private String name;
}
