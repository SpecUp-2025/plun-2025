package com.spec.plun.team.dto;

import java.util.List;

import lombok.Data;

@Data
public class TeamCreateRequest {
	private Integer userNo;
	private Integer teamNo;
	private String email;
	private String userName;
	private String teamName;
	private List<String> invite;

}
