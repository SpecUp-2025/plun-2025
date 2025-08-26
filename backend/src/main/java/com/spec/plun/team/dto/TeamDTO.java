package com.spec.plun.team.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class TeamDTO {
	private Integer teamNo;
	private String teamName;
	private Date createDate;

}
