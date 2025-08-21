package com.spec.plun.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
 * creatorUserId: 로그인 붙으면 생성자를 토큰에서 읽어 이 필드를 채우고
 * participantIds 중 creatorUserId 한 명만 C001, 나머지는 C002 로 저장하도록 Service에서 분기.
 * teamNo: 로그인/팀 기능 완성되면 필수값으로 승격하거나 서버에서 토큰 기반으로 주입
 * privateRoom: true 일 때만 roomPassword 필수 검증 추가(지금은 생략)
 */
@Data
@NoArgsConstructor      // MyBatis/역직렬화 안전
@AllArgsConstructor
@Builder
public class MeetingCreateRequest {

    private Integer teamNo;
    private String title;
    private LocalDateTime scheduledTime;
    private LocalDateTime scheduledEndTime;
    private Boolean privateRoom;
    private String roomPassword;
    private List<Integer> participantIds;

    // ▼ 로그인 붙인 뒤 사용할 예정(지금은 주석만 남겨둠)
    // private Integer creatorUserId; // TODO: 토큰에서 추출 → 생성자는 C001, 나머지는 C002
}
