package com.spec.plun.meeting.room;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeetingRoomMapper {
    int insert(MeetingRoom room);                           // room_no 자동 생성
    MeetingRoom findById(@Param("roomNo") Integer roomNo);  // PK 조회
    MeetingRoom findByCode(@Param("roomCode") String roomCode); // room_code 조회
    List<MeetingRoom> listRecent();
}
