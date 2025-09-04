package com.spec.plun.team.dto;

import lombok.Data;

@Data
public class TeamAcceptRequest {
	private Integer teamNo;
	private Integer userNo;
	private Integer invitedId;

}
