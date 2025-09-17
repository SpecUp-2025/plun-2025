package com.spec.plun.team.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TeamDTO {
	private Integer teamNo;
	private String teamName;
	private LocalDateTime createDate;

}
