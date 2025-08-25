// 파일: com/spec/plun/meeting/room/MeetingRoomService.java
package com.spec.plun.meeting.room;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ★ 추가

import com.spec.plun.meeting.dto.MeetingRoomListDto;
import com.spec.plun.meeting.room.MeetingRoomController.AuthzRes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingRoomService {

    private final MeetingRoomDAO meetingRoomDAO;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final java.security.SecureRandom random = new java.security.SecureRandom();

    /*
     * 회의방 생성 (기존 그대로 유지)
     */
    public Integer create(MeetingRoom room, String plainPassword) {
        if (room.getIsPrivate() == null) room.setIsPrivate("N");

        if (room.getScheduledTime() == null) {
            room.setScheduledTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        }

        if (room.getRoomCode() == null || room.getRoomCode().isBlank()) {
            room.setRoomCode(generateUniqueCode(8));
        }

        if ("Y".equals(room.getIsPrivate())) {
            if (plainPassword == null || plainPassword.isBlank()) {
                throw new IllegalArgumentException("비공개 방은 비밀번호가 필요합니다.");
            }
            room.setRoomPasswordHash(sha256(plainPassword)); // TODO: BCrypt/Argon2
        } else {
            room.setRoomPasswordHash(null);
        }

        meetingRoomDAO.insert(room);
        return room.getRoomNo();
    }

    /*
     * 종료되지 않은 회의 목록
     */
    public List<MeetingRoomListDto> listActiveByTeamAndMember(Integer teamNo, Integer userNo) {
        return meetingRoomDAO.selectActiveByTeamAndMember(LocalDateTime.now(), teamNo, userNo);
    }

    /*
     * 참가자 추가 (생성자=C001, 초대자=C002)
     */
    public void addParticipantsWithRoles(Integer roomNo, Integer creatorUserNo, List<Integer> allSelected) {
        if (roomNo == null) return;

        if (creatorUserNo != null) {
            meetingRoomDAO.insertParticipants(roomNo, "C001", List.of(creatorUserNo), null); // ★ 변경: now → null
        }

        if (allSelected != null && !allSelected.isEmpty()) {
            var invited = allSelected.stream()
                    .filter(uid -> !uid.equals(creatorUserNo))
                    .distinct()
                    .toList();
            if (!invited.isEmpty()) {
                meetingRoomDAO.insertParticipants(roomNo, "C002", invited, null); // ★ 변경: now → null
            }
        }
    }

    /*
     *방 생성 + 참가자 삽입을 한 번에 처리
     */
    public CreateResult createRoomAndParticipants(MeetingRoom room,
                                                  String plainPassword,
                                                  Integer creatorUserNo,
                                                  List<Integer> allSelected) {
        Integer roomNo = create(room, plainPassword); // 기존 create 재사용
        addParticipantsWithRoles(roomNo, creatorUserNo, allSelected); // 기존 메서드 재사용
        return new CreateResult(roomNo, room.getRoomCode());
    }

    public AuthzRes checkAuthz(String roomCode, Integer userNo) {
        var room = meetingRoomDAO.findByCode(roomCode);
        if (room == null) {
            return new AuthzRes(null, null, false);
        }
        String role = meetingRoomDAO.findRole(room.getRoomNo(), userNo);
        boolean ok = (role != null);
        return new AuthzRes(room.getTitle(), role, ok);
    }

    public record CreateResult(Integer roomNo, String roomCode) {}

    // ── 내부 유틸 ───────────────────────────────────
    private String generateUniqueCode(int len) {
        for (int i = 0; i < 5; i++) {
            String code = randomCode(len);
            if (meetingRoomDAO.findByCode(code) == null)
                return code;
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
