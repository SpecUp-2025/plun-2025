// 파일: com/spec/plun/meeting/room/MeetingRoomService.java
package com.spec.plun.meeting.room;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spec.plun.meeting.dto.MeetingRoomListDto;
import com.spec.plun.meeting.room.MeetingRoomController.AuthzRes;

import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingRoomService {

	private final MeetingRoomDAO meetingRoomDAO;

	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final java.security.SecureRandom random = new java.security.SecureRandom();

	public Integer create(MeetingRoom room, String plainPassword) {
		if (room.getIsPrivate() == null)
			room.setIsPrivate("N");

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
			room.setRoomPasswordHash(sha256(plainPassword));
		} else {
			room.setRoomPasswordHash(null);
		}

		meetingRoomDAO.insert(room);
		return room.getRoomNo();
	}

	public List<MeetingRoomListDto> listActiveByTeamAndMember(Integer teamNo, Integer userNo) {
		return meetingRoomDAO.selectActiveByTeamAndMember(LocalDateTime.now(), teamNo, userNo);
	}

	public void addParticipantsWithRoles(Integer roomNo, Integer creatorUserNo, List<Integer> allSelected) {
		if (roomNo == null)
			return;

		if (creatorUserNo != null) {
			meetingRoomDAO.insertParticipants(roomNo, "C001", List.of(creatorUserNo), null);
		}

		if (allSelected != null && !allSelected.isEmpty()) {
			var invited = allSelected.stream().filter(uid -> !uid.equals(creatorUserNo)).distinct().toList();
			if (!invited.isEmpty()) {
				meetingRoomDAO.insertParticipants(roomNo, "C002", invited, null);
			}
		}
	}

	public CreateResult createRoomAndParticipants(MeetingRoom room, String plainPassword, Integer creatorUserNo,
			List<Integer> allSelected) {
		Integer roomNo = create(room, plainPassword);
		addParticipantsWithRoles(roomNo, creatorUserNo, allSelected);
		return new CreateResult(roomNo, room.getRoomCode());
	}

	public AuthzRes checkAuthz(String roomCode, Integer userNo) {
		var room = meetingRoomDAO.findByCode(roomCode);
		if (room == null)
			return new AuthzRes(null, null, false);
		String role = meetingRoomDAO.findRole(room.getRoomNo(), userNo);
		boolean ok = (role != null);
		return new AuthzRes(room.getTitle(), role, ok);
	}

	public record CreateResult(Integer roomNo, String roomCode) {
	}

	public void logEnter(String roomCode, Integer userNo, String joinedAtIso) {
		var room = meetingRoomDAO.findByCode(roomCode);
		if (room == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회의방을 찾을 수 없습니다.");

		LocalDateTime joinedAt = parseIsoToLocal(joinedAtIso);

		int updated = meetingRoomDAO.updateJoinTime(room.getRoomNo(), userNo, joinedAt);
		if (updated == 0) {
			// 참가자 테이블에 (room_no, user_no) 가 없다면 생성 시 참가자 삽입이 누락된 케이스
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "참가자 정보를 찾을 수 없습니다.");
		}
	}

	public void logLeave(String roomCode, Integer userNo, String joinedAtIso, String leftAtIso) {
		var room = meetingRoomDAO.findByCode(roomCode);
		if (room == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회의방을 찾을 수 없습니다.");

		parseIsoToLocal(joinedAtIso);
		LocalDateTime leftAt = parseIsoToLocal(leftAtIso);

		int updated = meetingRoomDAO.updateOutTime(room.getRoomNo(), userNo, leftAt);
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "참가자 정보를 찾을 수 없습니다.");
		}
	}

	// 내부 유틸

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

	private LocalDateTime parseIsoToLocal(String iso) {
		if (iso == null || iso.isBlank())
			return null;
		try {
			// '2025-08-25T08:12:30Z' 같은 형태도 허용
			return OffsetDateTime.parse(iso).toLocalDateTime();
		} catch (DateTimeParseException e) {
			try {
				// 'YYYY-MM-DDTHH:mm' 로컬 문자열 형태도 허용
				return LocalDateTime.parse(iso);
			} catch (DateTimeParseException e2) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 시간 형식: " + iso);
			}
		}
	}
}
