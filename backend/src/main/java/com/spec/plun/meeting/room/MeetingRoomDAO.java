package com.spec.plun.meeting.room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.meeting.dto.MeetingRoomListDto;

@Mapper
public interface MeetingRoomDAO {

    /* ===== 회의방 기본 ===== */

    // 회의방 INSERT (useGeneratedKeys=true 로 roomNo 세팅되게 매퍼에서 처리)
    int insert(MeetingRoom room);

    // room_code 중복 체크용 조회 - 서비스 generateUniqueCode()에서 사용
    MeetingRoom findByCode(@Param("roomCode") String roomCode);

    // 종료되지 않은 회의 목록
    List<MeetingRoomListDto> selectActiveByTeamAndMember(@Param("now") LocalDateTime now,
                                                         @Param("teamNo") Integer teamNo,
                                                         @Param("userNo") Integer userNo);

    String findRole(@Param("roomNo") Integer roomNo, @Param("userNo") Integer userNo);

    // 입·퇴장 기록
    int updateJoinTime(@Param("roomNo") Integer roomNo,
                       @Param("userNo") Integer userNo,
                       @Param("joinedAt") LocalDateTime joinedAt);

    int updateOutTime(@Param("roomNo") Integer roomNo,
                      @Param("userNo") Integer userNo,
                      @Param("leftAt") LocalDateTime leftAt);

    // 회의 참여자 일괄 INSERT (역할 구분: C001=생성자, C002=참여자)
    int insertParticipants(@Param("roomNo") Integer roomNo,
                           @Param("roleNo") String roleNo,
                           @Param("userIds") List<Integer> userIds,
                           @Param("joinTime") LocalDateTime joinTime);

    // 회의방에 cal_detail_no 메모
    int updateCalDetailNo(@Param("roomNo") Integer roomNo,
                          @Param("calDetailNo") Integer calDetailNo);



    /* ===== 팀 달력 / 달력상세 ===== */

    // 팀 달력 찾기 (없으면 NULL)
    Integer findCalNoByTeamNo(@Param("teamNo") Integer teamNo);

    // 팀 달력 생성 (INSERT)
    //  - 반환값은 영향 행 수(int). 생성된 cal_no는 이후 selectLastInsertId()로 조회
    int insertTeamCalendar(@Param("teamNo") Integer teamNo,
                           @Param("userNo") Integer userNo);

    // 달력상세 생성 (INSERT)
    //  - 반환값은 영향 행 수(int). 생성된 cal_detail_no는 이후 selectLastInsertId()로 조회
    int insertCalendarDetail(@Param("calNo") Integer calNo,
                             @Param("title") String title,
                             @Param("contents") String contents,
                             @Param("startDate") LocalDate startDate,
                             @Param("startTime") LocalTime startTime,
                             @Param("endDate") LocalDate endDate,
                             @Param("endTime") LocalTime endTime,
                             @Param("regUserNo") Integer regUserNo);

    // 마지막 AUTO_INCREMENT 값 조회 (커넥션 단위) - MySQL
    Integer selectLastInsertId();



    /* ===== 달력상세 참여자 ===== */

    // 달력상세 참여자 벌크 INSERT (생성자 제외한 초대 인원만)
    int insertCalendarDetailParticipants(@Param("calDetailNo") Integer calDetailNo,
                                         @Param("userIds") List<Integer> userIds);
}
