// 파일: com/spec/plun/meeting/room/MeetingRoomDAO.java
package com.spec.plun.meeting.room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.meeting.dto.MeetingRoomListDto;
import com.spec.plun.meeting.dto.ParticipantDto;

@Mapper
public interface MeetingRoomDAO {

    /* ===== 생성/조회(기존) ===== */
    int insert(MeetingRoom room); // useGeneratedKeys 로 roomNo 세팅
    MeetingRoom findByCode(@Param("roomCode") String roomCode);
    Integer selectCreatorUserNo(@Param("roomNo") Integer roomNo);
    List<ParticipantDto> selectParticipantsWithNames(@Param("roomNo") Integer roomNo);

    List<MeetingRoomListDto> selectActiveByTeamAndMember(@Param("now") LocalDateTime now,
                                                         @Param("teamNo") Integer teamNo,
                                                         @Param("userNo") Integer userNo);

    String findRole(@Param("roomNo") Integer roomNo, @Param("userNo") Integer userNo);

    int updateJoinTime(@Param("roomNo") Integer roomNo,
                       @Param("userNo") Integer userNo,
                       @Param("joinedAt") LocalDateTime joinedAt);

    int updateOutTime(@Param("roomNo") Integer roomNo,
                      @Param("userNo") Integer userNo,
                      @Param("leftAt") LocalDateTime leftAt);

    int insertParticipants(@Param("roomNo") Integer roomNo,
                           @Param("roleNo") String roleNo,           // 'C001' | 'C002'
                           @Param("userIds") List<Integer> userIds,
                           @Param("joinTime") LocalDateTime joinTime);

    int updateCalDetailNo(@Param("roomNo") Integer roomNo,
                          @Param("calDetailNo") Integer calDetailNo);

    Integer findCalNoByTeamNo(@Param("teamNo") Integer teamNo);
    int insertTeamCalendar(@Param("teamNo") Integer teamNo,
                           @Param("userNo") Integer userNo);

    int insertCalendarDetail(@Param("calNo") Integer calNo,
                             @Param("title") String title,
                             @Param("contents") String contents,
                             @Param("startDate") LocalDate startDate,
                             @Param("startTime") LocalTime startTime,
                             @Param("endDate") LocalDate endDate,
                             @Param("endTime") LocalTime endTime,
                             @Param("regUserNo") Integer regUserNo);

    Integer selectLastInsertId();

    int insertCalendarDetailParticipants(@Param("calDetailNo") Integer calDetailNo,
                                         @Param("userIds") List<Integer> userIds);

    /* ===== ⬇ 신규: 수정/삭제 지원 ===== */
    MeetingRoom findByRoomNo(@Param("roomNo") Integer roomNo);

    int updateMeetingRoom(@Param("roomNo") Integer roomNo,
                          @Param("title") String title,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    int updateCalendarDetail(@Param("calDetailNo") Integer calDetailNo,
                             @Param("title") String title,
                             @Param("startDate") LocalDate startDate,
                             @Param("startTime") LocalTime startTime,
                             @Param("endDate") LocalDate endDate,
                             @Param("endTime") LocalTime endTime);

    List<Integer> selectParticipantUserNos(@Param("roomNo") Integer roomNo);
    int deleteParticipants(@Param("roomNo") Integer roomNo,
                           @Param("userNos") List<Integer> userNos);
    int deleteParticipantsAll(@Param("roomNo") Integer roomNo);

    List<Integer> selectCalendarDetailParticipantUserNos(@Param("calDetailNo") Integer calDetailNo);
    int deleteCalendarDetailParticipants(@Param("calDetailNo") Integer calDetailNo,
                                         @Param("userNos") List<Integer> userNos);
    int deleteCalendarDetailParticipantsAll(@Param("calDetailNo") Integer calDetailNo);

    int deleteCalendarDetail(@Param("calDetailNo") Integer calDetailNo);
    int deleteMeetingRoom(@Param("roomNo") Integer roomNo);
}
