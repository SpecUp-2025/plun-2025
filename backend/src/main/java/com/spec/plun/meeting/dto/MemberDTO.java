package com.spec.plun.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원 간단 정보 DTO
 *
 * 어디에 쓰나?
 * - 회의 생성 화면에서 "참여자 선택" 목록을 채우기 위해 /api/members 응답에 사용.
 *
 * 나중에 바꿀 점(TODO):
 * - 로그인/팀 기능 붙으면: /api/members?teamNo=... 처럼 팀별 조회를 하게 될 수 있음.
 *   그래도 이 DTO 자체는 그대로 재사용 가능(필드 추가 필요 시 확장).
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDTO {
    private Integer userNo;  // tb_member.user_no
    private String  name;    // tb_member.name
    private String  email;   // tb_member.email
}
