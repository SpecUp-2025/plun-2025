package com.spec.plun.member.DTO;

import lombok.Data;

@Data
public class SetPasswordRequest {
	private String email;
	private String currentPassword;
	private String password;

}
