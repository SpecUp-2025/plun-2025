package com.spec.plun.meeting.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MeetingRoomServiceTest {

    @Autowired
    private MeetingRoomService service;

    @Test
    void insert_and_findById() {
        // 1) 새 회의방 객체 준비 (필수: title)
        MeetingRoom room = new MeetingRoom();
        room.setTitle("테스트-" + System.currentTimeMillis());
        // teamNo, scheduledTime 등은 생략 가능 (XML에서 기본값 처리)

        // 2) 생성 → PK(roomNo) 반환
        Integer roomNo = service.create(room);
        assertNotNull(roomNo, "생성 후 roomNo가 null이면 안 됨");

        // 3) 조회 → 필드 검증
        MeetingRoom found = service.findById(roomNo);
        assertNotNull(found, "findById가 null이면 안 됨");
        assertEquals(room.getTitle(), found.getTitle());
        assertNotNull(found.getRoomCode(), "roomCode는 생성되어 있어야 함");
    }
}
