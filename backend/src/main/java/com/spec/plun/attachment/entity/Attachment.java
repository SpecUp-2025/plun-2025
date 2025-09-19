package com.spec.plun.attachment.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Attachment {
	private String attachmentNo; 
	private Integer messageNo;
	private String originalName;
	private String fileName;
	private String path;
	private Integer size;
	private String contentType;
	private String extension;
	private LocalDateTime createDate;
	private LocalDateTime deleteDate;

}
