package com.spec.plun.team.dto;

import java.util.List;

import lombok.Data;

@Data
public class MemberInviteRequest {
	private Integer userNo;
	private Integer teamNo;
	private String email;
	private String userName;
	private List<String> invite;
}
