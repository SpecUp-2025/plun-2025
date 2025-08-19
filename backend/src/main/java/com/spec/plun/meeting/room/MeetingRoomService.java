package com.spec.plun.meeting.room;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomMapper mapper;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    /** 방 생성: roomCode 없으면 생성, INSERT 후 roomNo 반환 */
    public Integer create(MeetingRoom room) {
        if (room.getIsPrivate() == null) room.setIsPrivate("N");
        if (room.getRoomCode() == null || room.getRoomCode().isBlank()) {
            room.setRoomCode(generateUniqueCode(8)); // 기본 8자
        }
        mapper.insert(room);              // useGeneratedKeys 로 room.roomNo 채워짐
        return room.getRoomNo();
    }

    /** 단건 조회 */
    public MeetingRoom findById(Integer roomNo) {
        return mapper.findById(roomNo);
    }

    /** 최근 회의방 목록 조회 */
    public List<MeetingRoom> listRecent() {
        return mapper.listRecent();
    }
    
    // ----------------- 내부 유틸 -----------------
    private String generateUniqueCode(int len) {
        // 중복 가능성 낮지만 혹시 모르니 몇 번 체크
        for (int i = 0; i < 5; i++) {
            String code = randomCode(len);
            if (mapper.findByCode(code) == null) return code;
        }
        return randomCode(len + 2);
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
