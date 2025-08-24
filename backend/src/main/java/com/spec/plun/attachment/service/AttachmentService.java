package com.spec.plun.attachment.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.spec.plun.attachment.dao.AttachmentDAO;
import com.spec.plun.attachment.entity.Attachment;

@Service
public class AttachmentService {
	
	@Autowired
	private AttachmentDAO attachmentDAO;
	
	// 실제 파일 저장 위치 (나중에 설정 파일로 빼기)
	private final String uploadPath = "C:/upload/chat"; // 본인 pc에 맞게 설정 변경

	// 파일 삭제
	public boolean deleteAttachmentById(String attachmentNo) {
		// 1. DB에서 첨부파일 정보 조회
	    Attachment attachment = attachmentDAO.getAttachmentById(attachmentNo);
	    if (attachment == null) return false;

	    // 2. 실제 파일 삭제
	    Path filePath = Paths.get(attachment.getPath());
	    try {
	        Files.deleteIfExists(filePath);
	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new RuntimeException("파일 삭제 실패");
	    }

	    // 3. DB에서 메타데이터 삭제
	    attachmentDAO.deleteAttachment(attachmentNo);
	    return true;
	}

	// 파일 다운로드
	public Attachment getAttachmentById(String attachmentNo) {
		return attachmentDAO.getAttachmentById(attachmentNo);
		
	}
	
	// 파일 업로드
	public Attachment saveFile(MultipartFile file, int messageNo) throws IOException{
		
		// 1. 파일 저장 이름 생성
		String uuid = UUID.randomUUID().toString();
		String originalName = file.getOriginalFilename();
		String fileName = uuid + "_" + originalName;
		
		// 2. 실제 파일 저장 경로
		File saveDir = new File(uploadPath);
		if(!saveDir.exists()) {
			saveDir.mkdirs(); // 디렉토리 없으면 생성
	}
		
		File dest = new File(saveDir, fileName);
		file.transferTo(dest); // 파일 저장
		
		// 3. Attachment 객체에 정보 담기
		Attachment attachment = new Attachment();
		attachment.setAttachmentNo(uuid);
        attachment.setMessageNo(messageNo);
        attachment.setOriginalName(originalName);
        attachment.setFileName(fileName);
        attachment.setPath(dest.getAbsolutePath());
        attachment.setSize((int) file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setExtension(getExtension(originalName));
        attachment.setCreateDate(LocalDateTime.now());

        // 4. DB에 저장
        attachmentDAO.insertAttachment(attachment);

        return attachment;
		
	}
	
    // 파일 확장자 추출
    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf(".");
        return idx == -1 ? "" : fileName.substring(idx + 1);
    }
    
}
