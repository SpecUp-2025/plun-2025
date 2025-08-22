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
 * - 프론트에서 "회의 생성하기"를 누르면 보내는 JSON 바디를 받음
 * - Service에서 이 데이터를 사용해 tb_meeting_room INSERT + tb_meeting_participant INSERT
 *
 * 시간 주의
 *  LocalDateTime 은 타임존 정보가 없습니다.
 *  프론트의 datetime-local 값을 그대로 받으면 "서버 타임존" 기준으로 DB에 저장됩니다.
 *   (운영 정책 확정 시 KST/UTC 변환 로직을 Service에 추가 예정)
 *
 * 지금 단계의 임시 정책
 * 로그인 미완 → "생성자 식별 불가" → 참가자 전원 role_no = 'C001' 로 INSERT (Service에서 처리)
 *
 * (TODO)
 * privateRoom: true 일 때만 roomPassword 필수 검증 추가(지금은 생략)
 */
@Data
@NoArgsConstructor      // MyBatis/역직렬화 안전
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
    private Boolean privateRoom;
    private String roomPassword;
    private List<Integer> participantIds;
    private Integer creatorUserNo;

}
