package com.spec.plun.meeting.room;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spec.plun.meeting.dto.MeetingRoomListDto;
import com.spec.plun.meeting.room.MeetingRoomController.AuthzRes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

	private final MeetingRoomDAO meetingRoomDAO;

	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final java.security.SecureRandom random = new java.security.SecureRandom();

	/**
	 * 회의방 생성 - is_private 기본값 처리 - room_code 자동 생성(중복 방지) - 비공개 방일 때 평문 비밀번호 → 해시
	 * 저장 - INSERT 후 roomNo 반환
	 */
	public Integer create(MeetingRoom room, String plainPassword) {
		if (room.getIsPrivate() == null) room.setIsPrivate("N");

	    if (room.getScheduledTime() == null) {
	        room.setScheduledTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
	    }
	    
		if (room.getRoomCode() == null || room.getRoomCode().isBlank()) {
			room.setRoomCode(generateUniqueCode(8));
		}

		// 🔐 비공개 방 비밀번호 처리 (평문 → 해시)
		if ("Y".equals(room.getIsPrivate())) {
			if (plainPassword == null || plainPassword.isBlank()) {
				throw new IllegalArgumentException("비공개 방은 비밀번호가 필요합니다.");
			}
			// TODO(운영 보안): SHA-256 → BCrypt/Argon2 + 랜덤 솔트로 교체
			room.setRoomPasswordHash(sha256(plainPassword));
		} else {
			room.setRoomPasswordHash(null);
		}

		meetingRoomDAO.insert(room);
	    return room.getRoomNo();
	}

	/** 종료되지 않은 회의 목록 (팀 + 특정 사용자에게 초대된 회의만) */
	public List<MeetingRoomListDto> listActiveByTeamAndMember(Integer teamNo, Integer userNo) {
		return meetingRoomDAO.selectActiveByTeamAndMember(LocalDateTime.now(), teamNo, userNo);
	}

	/** 참가자 추가 (생성자 = C001, 초대자 = C002) */
    public void addParticipantsWithRoles(Integer roomNo, Integer creatorUserNo, List<Integer> allSelected) {
        if (roomNo == null) return;
        var now = LocalDateTime.now();

        // 생성자 = C001
        if (creatorUserNo != null) {
            meetingRoomDAO.insertParticipants(roomNo, "C001", List.of(creatorUserNo), now);
        }

        // 초대자 = C002 (전체선택 - 생성자)
        if (allSelected != null && !allSelected.isEmpty()) {
            var invited = allSelected.stream()
                    .filter(uid -> !uid.equals(creatorUserNo))
                    .distinct()
                    .toList();
            if (!invited.isEmpty()) {
                meetingRoomDAO.insertParticipants(roomNo, "C002", invited, now);
            }
        } 
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
