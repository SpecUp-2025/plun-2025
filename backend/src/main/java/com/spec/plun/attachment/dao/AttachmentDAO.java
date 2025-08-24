package com.spec.plun.attachment.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.spec.plun.attachment.entity.Attachment;

@Mapper
public interface  AttachmentDAO {
	
	void insertAttachment(Attachment attachment);
	
	List<Attachment> getAttachmentsByMessageNo(int messageNo);
	
	Attachment getAttachmentById(String attachmentNo);
	
	void deleteAttachment(String attachmentNo);

}
