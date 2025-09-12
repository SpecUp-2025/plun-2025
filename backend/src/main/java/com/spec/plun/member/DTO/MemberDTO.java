package com.spec.plun.member.DTO;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MemberDTO {
	private Integer userNo;
	private String password;
	private String email;
	private String name;
	private LocalDateTime updateDate;
	private Integer loginTypeNo;

}
