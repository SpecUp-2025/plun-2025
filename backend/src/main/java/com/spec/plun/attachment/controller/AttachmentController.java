package com.spec.plun.attachment.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.spec.plun.attachment.entity.Attachment;
import com.spec.plun.attachment.service.AttachmentService;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {

	@Autowired
	private AttachmentService attachmentService;
	
	// 파일 삭제
	@DeleteMapping("/delete/{attachmentNo}")
	public ResponseEntity<?> deleteAttachment(@PathVariable("attachmentNo") String attachmentNo){
		try {
			boolean deleted = attachmentService.deleteAttachmentById(attachmentNo);
			if(deleted) {
				return ResponseEntity.ok().body("파일 삭제 완료");
				} else {
					return ResponseEntity.status(404).body("파일을 찾을 수 없습니다.");
				}
		} catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(500).body("파일 삭제 중 오류 발생");
	    }
	}
	
	
	// 파일 다운로드
	@GetMapping("/download/{attachmentNo}")
	public ResponseEntity<Resource> downloadFile(@PathVariable("attachmentNo") String attachmentNo){
		try {
			// 1. 첨부파일 정보를 DB에서 조회
			Attachment attachment = attachmentService.getAttachmentById(attachmentNo);
			
			// 2. 첨부파일이 존재하지 않을 경우 404 응답 반환
			if (attachment == null) {
				System.out.println("첨부파일을 찾을 수 없습니다: " + attachmentNo);
				return ResponseEntity.notFound().build();
			}
			// 3. 파일 경로 생성 (실제 파일 시스템 경로)
	        Path filePath = Paths.get(attachment.getPath()); // 실제 경로에서 파일 가져오기
	        System.out.println("파일 경로: " + filePath.toAbsolutePath());
	        
	        // 4. 파일 리소스 생성 (UrlResource를 통해 파일을 스트림 형태로 제공)
	        Resource resource = new UrlResource(filePath.toUri());
	        
	        // 5. 파일이 실제 존재하지 않는 경우 404 응답 반환
	        if(!resource.exists()) {
	        	System.out.println("리소스가 존재하지 않음: " + resource.getFilename());
	        	return ResponseEntity.notFound().build();   	
	        }
	        
	        // 6. 다운로드 시 한글 파일명이 깨지지 않도록 URL 인코딩 처리
	        String encodedFileName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
	        
	        // 7. ResponseEntity 생성 및 헤더 세팅
	        // Content-Disposition 헤더에 인코딩된 파일명을 포함하여 첨부파일로 다운로드 되도록 설정
	        return ResponseEntity.ok()
	        		.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
	                .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                .body(resource);
	        
		}  catch  (Exception e) {
			// 8. 예외 발생 시 로그 출력 및 500 서버 에러 응답 반환
			System.err.println("다운로드 중 예외 발생");
	        e.printStackTrace();
	        return ResponseEntity.status(500).build();
		}
	}
	
	// 파일 업로드
	@PostMapping("/upload")
	public ResponseEntity<Attachment> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("messageNo") Integer messageNo){
        
		try {
            Attachment savedAttachment = attachmentService.saveFile(file, messageNo);
            return ResponseEntity.ok(savedAttachment);
        } catch (Exception e) {
            e.printStackTrace(); // 개발 중이라면 에러 출력
            return ResponseEntity.status(500).build();
        }
    }
}