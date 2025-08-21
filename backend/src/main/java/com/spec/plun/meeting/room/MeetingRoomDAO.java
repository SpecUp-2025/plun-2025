package com.spec.plun.meeting.room;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeetingRoomDAO {

    /**
     * 회의방 INSERT
     * - XML에서 useGeneratedKeys(or selectKey)로 room_no → room.roomNo 에 채워 넣어야 함
     *   ex) <insert useGeneratedKeys="true" keyProperty="roomNo"> ... </insert>
     */
    int insert(MeetingRoom room);

    /**
     * room_code 중복 체크용 조회
     * - 서비스의 generateUniqueCode()에서 사용
     */
    MeetingRoom findByCode(@Param("roomCode") String roomCode);

    /** 참가자 일괄 INSERT (임시 정책: 전원 C001) */
    int insertParticipants(@Param("roomNo") Integer roomNo,
                           @Param("roleNo") String roleNo,           // TODO: 로그인 붙으면 C001/C002 분기
                           @Param("userIds") List<Integer> userIds,
                           @Param("joinTime") LocalDateTime joinTime);
    
    /** 종료되지 않은 회의 목록 (now 기준) */
    List<MeetingRoom> selectActive(@Param("now") LocalDateTime now);
    // TODO(필터): teamNo / userNo 조건 필요해지면 오버로드 추가
    
    // 목록/필터 API가 필요해지면 아래 같은 메서드를 추가:
    // List<MeetingRoom> selectActiveByUser(@Param("userNo") Integer userNo,
    //                                      @Param("now") LocalDateTime now);
    // List<MeetingRoom> selectActiveByTeam(@Param("teamNo") Integer teamNo,
    //                                      @Param("now") LocalDateTime now);
}
