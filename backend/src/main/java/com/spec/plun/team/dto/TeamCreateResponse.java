package com.spec.plun.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamCreateResponse {
	private String teamName;
	private Integer teamNo;

}
