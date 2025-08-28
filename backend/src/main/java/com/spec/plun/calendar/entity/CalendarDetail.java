package com.spec.plun.calendar.entity;

import lombok.Data;

@Data
public class CalendarDetail {
	
    private Integer  calDetailNo;
    private Integer  calNo;
    private String contents;
    private String startDate;
    private String startTime;
    private String endDate;
    private String endTime;
    private String deleteYn;
    private Integer  regUserNo;
    private String regDt;
    private Integer  updateUserNo;
    private String updateDt;
    private String title;

}
