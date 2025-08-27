package com.spec.plun.meeting.room;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.meeting.dto.MeetingRoomListDto;

@Mapper
public interface MeetingRoomDAO {

	// 회의방 INSERT
	int insert(MeetingRoom room);

	// room_code 중복 체크용 조회 - 서비스의 generateUniqueCode()에서 사용
	MeetingRoom findByCode(@Param("roomCode") String roomCode);

	// 참가자 일괄 INSERT
	int insertParticipants(@Param("roomNo") Integer roomNo, @Param("roleNo") String roleNo,
			@Param("userIds") List<Integer> userIds, @Param("joinTime") LocalDateTime joinTime);

	// 종료되지 않은 회의 목록
	List<MeetingRoomListDto> selectActiveByTeamAndMember(@Param("now") LocalDateTime now,
			@Param("teamNo") Integer teamNo, @Param("userNo") Integer userNo);

	String findRole(@Param("roomNo") Integer roomNo, @Param("userNo") Integer userNo);

	// 입·퇴장 기록
	int updateJoinTime(@Param("roomNo") Integer roomNo, @Param("userNo") Integer userNo,
			@Param("joinedAt") LocalDateTime joinedAt);

	int updateOutTime(@Param("roomNo") Integer roomNo, @Param("userNo") Integer userNo,
			@Param("leftAt") LocalDateTime leftAt);
}
