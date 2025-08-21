package com.spec.plun.meeting.room;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomDAO meetingRoomDAO;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final java.security.SecureRandom random = new java.security.SecureRandom();

    /**
     * 회의방 생성
     * - is_private 기본값 처리
     * - room_code 자동 생성(중복 방지)
     * - (리팩터) 비공개 방일 때 평문 비밀번호 → 해시로 저장
     * - INSERT 후 roomNo 반환
     */
    public Integer create(MeetingRoom room, String plainPassword) {
        if (room.getIsPrivate() == null) room.setIsPrivate("N");

        if (room.getRoomCode() == null || room.getRoomCode().isBlank()) {
            room.setRoomCode(generateUniqueCode(8));
        }

        // 🔐 비공개 방 비밀번호 처리 (평문 → 해시)
        if ("Y".equals(room.getIsPrivate())) {
            if (plainPassword == null || plainPassword.isBlank()) {
                throw new IllegalArgumentException("비공개 방은 비밀번호가 필요합니다.");
            }
            // TODO(운영 보안): SHA-256 → BCrypt 등 강한 해시 + 랜덤 솔트 적용
            room.setRoomPasswordHash(sha256(plainPassword));
        } else {
            room.setRoomPasswordHash(null);
        }

        // TODO(DB 정책): scheduled_time null → NOW()는 Mapper XML에서 COALESCE(NOW())로 처리 중

        meetingRoomDAO.insert(room); // useGeneratedKeys로 roomNo 채움
        return room.getRoomNo();
    }

    /** 종료되지 않은 회의 목록 (서버 시각 기준) */
    public List<MeetingRoom> listActive() {
        return meetingRoomDAO.selectActive(LocalDateTime.now());
    }

    /** 참가자 일괄 추가 (임시: 전원 C001) */
    public void addParticipantsAllAsHost(Integer roomNo, List<Integer> userIds) {
        if (roomNo == null || userIds == null || userIds.isEmpty()) return;
        meetingRoomDAO.insertParticipants(roomNo, "C001", userIds, LocalDateTime.now());
        // TODO(로그인 붙이면): 생성자만 C001, 나머지는 C002로 분기
    }

    // ── 내부 유틸 ───────────────────────────────────
    private String generateUniqueCode(int len) {
        for (int i = 0; i < 5; i++) {
            String code = randomCode(len);
            if (meetingRoomDAO.findByCode(code) == null) return code;
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

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
