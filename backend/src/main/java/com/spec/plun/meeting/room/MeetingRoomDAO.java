package com.spec.plun.meeting.room;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.meeting.dto.MeetingRoomListDto;

@Mapper
public interface MeetingRoomDAO {

    /**
     * 회의방 INSERT
     * - XML에서 useGeneratedKeys(or selectKey)로 room_no → room.roomNo에 채워 넣어야 함
     *   ex) <insert useGeneratedKeys="true" keyProperty="roomNo"> ... </insert>
     */
    int insert(MeetingRoom room);

    /**
     * room_code 중복 체크용 조회
     * - 서비스의 generateUniqueCode()에서 사용
     */
    MeetingRoom findByCode(@Param("roomCode") String roomCode);

    /**
     * 참가자 일괄 INSERT (임시 정책: 전원 C001)
     * TODO: 로그인 붙이면 생성자 C001 / 초대자 C002로 분기 예정
     */
    int insertParticipants(@Param("roomNo") Integer roomNo,
                           @Param("roleNo") String roleNo,
                           @Param("userIds") List<Integer> userIds,
                           @Param("joinTime") LocalDateTime joinTime);

    /**
     * 종료되지 않은 회의 목록 (now 기준)
     * - 팀 + 특정 사용자에게 초대된 회의만
     * - 프론트 0단계: teamNo + userNo로 호출
     */
    List<MeetingRoomListDto> selectActiveByTeamAndMember(@Param("now") LocalDateTime now,
                                                         @Param("teamNo") Integer teamNo,
                                                         @Param("userNo") Integer userNo);

    String findRole(@Param("roomNo") Integer roomNo, @Param("userNo") Integer userNo);
}
