package com.spec.plun.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 회의 생성 요청 DTO
 *
 * 프론트에서 "회의 생성하기"로 넘어오는 JSON 바디를 매핑.
 * 비밀번호/비공개 개념은 제거됨 → 초대(참가자) 여부로만 권한 체크.
 *
 * 시간 주의:
 *  - LocalDateTime은 타임존 정보가 없음.
 *  - 현재는 프론트의 'datetime-local' 값을 서버 타임존 기준으로 저장.
 *    (운영 정책 확정 시 KST/UTC 변환 로직을 Service에 추가 가능)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingCreateRequest {

    private Integer teamNo;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @DateTimeFormat(iso = ISO.DATE_TIME, pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @DateTimeFormat(iso = ISO.DATE_TIME, pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledEndTime;

    private List<Integer> participantIds;  // 생성자 포함해서 보내도 되고, 서버에서 재보정 가능
    private Integer creatorUserNo;
}
