package com.spec.plun.team.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TeamsInvitationDTO {
	private Integer invitedId;
	private String teamName;
	private Integer teamNo;
	private LocalDateTime createDate;
	private String invitedEmail;
	
}