package com.spec.plun.member.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ResetPasswordRequest {
	@NotBlank
	private String email;
	@NotBlank
	private String password;
	

	

}
