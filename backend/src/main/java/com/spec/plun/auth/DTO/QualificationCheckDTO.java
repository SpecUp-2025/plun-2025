package com.spec.plun.auth.DTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QualificationCheckDTO {
	private final String email;
	private final String password;
	
}
