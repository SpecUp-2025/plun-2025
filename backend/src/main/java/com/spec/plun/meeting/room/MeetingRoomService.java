// 파일: com/spec/plun/meeting/room/MeetingRoomService.java
package com.spec.plun.meeting.room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spec.plun.meeting.dto.MeetingRoomListDto;
import com.spec.plun.meeting.dto.RoomDetailRes;
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

	/* ---------------- 기본 방 생성(코드/시간 기본값 설정) ---------------- */

	public Integer create(MeetingRoom room) {
		if (room.getScheduledTime() == null) {
			room.setScheduledTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
		}

		if (room.getRoomCode() == null || room.getRoomCode().isBlank()) {
			room.setRoomCode(generateUniqueCode(8));
		}

		meetingRoomDAO.insert(room);
		return room.getRoomNo();
	}

	/* ---------------- 참여자(회의 역할) 등록 ---------------- */

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

	/* ---------------- 회의방 + 달력상세 + 달력참여자(생성자 제외) 생성 ---------------- */

	public CreateResult createRoomWithCalendar(MeetingRoom room, Integer creatorUserNo, List<Integer> participantIds) {
		// 1) 회의방 생성
		Integer roomNo = create(room);

		// 2) 팀 달력 get-or-create
		Integer calNo = getOrCreateTeamCalendar(room.getTeamNo(), creatorUserNo);

		// 3) 달력상세 생성 (contents 초기 NULL, reg_user_no = 생성자)
		LocalDate sDate = room.getScheduledTime().toLocalDate();
		LocalTime sTime = room.getScheduledTime().toLocalTime();
		LocalDate eDate = room.getScheduledEndTime().toLocalDate();
		LocalTime eTime = room.getScheduledEndTime().toLocalTime();

		meetingRoomDAO.insertCalendarDetail(calNo, room.getTitle(), null, // contents
				sDate, sTime, eDate, eTime, creatorUserNo);
		Integer calDetailNo = meetingRoomDAO.selectLastInsertId(); // ← 방금 INSERT한 PK 회수

		// 4) 회의방에 cal_detail_no 메모(수정/삭제 용이)
		meetingRoomDAO.updateCalDetailNo(roomNo, calDetailNo);

		// 5) 회의참여자 저장 (중복 제거 + 생성자 보장)
		LinkedHashSet<Integer> uniq = new LinkedHashSet<>(participantIds == null ? List.of() : participantIds);
		if (creatorUserNo != null)
			uniq.add(creatorUserNo);
		List<Integer> all = List.copyOf(uniq);
		addParticipantsWithRoles(roomNo, creatorUserNo, all);

		// 6) 달력상세참여자: 생성자 제외하고 INSERT
		List<Integer> invitedOnly = all.stream().filter(u -> !u.equals(creatorUserNo)).toList();
		if (!invitedOnly.isEmpty()) {
			meetingRoomDAO.insertCalendarDetailParticipants(calDetailNo, invitedOnly);
		}

		// roomCode는 create()에서 세팅되어 있음
		return new CreateResult(roomNo, room.getRoomCode(), calDetailNo);
	}

	// (하위호환) 기존 컨트롤러가 호출하던 메서드 → 새 메서드로 위임
	public CreateResult createRoomAndParticipants(MeetingRoom room, Integer creatorUserNo, List<Integer> allSelected) {
		return createRoomWithCalendar(room, creatorUserNo, allSelected);
	}

	/* ---------------- 조회/인증/로그 (기존 유지) ---------------- */

	public List<MeetingRoomListDto> listActiveByTeamAndMember(Integer teamNo, Integer userNo) {
		return meetingRoomDAO.selectActiveByTeamAndMember(LocalDateTime.now(), teamNo, userNo);
	}

	public AuthzRes checkAuthz(String roomCode, Integer userNo) {
		var room = meetingRoomDAO.findByCode(roomCode);
		if (room == null)
			return new AuthzRes(null, null, false);
		String role = meetingRoomDAO.findRole(room.getRoomNo(), userNo);
		boolean ok = (role != null);
		return new AuthzRes(room.getTitle(), role, ok);
	}

	public void updateRoomAndCalendar(Integer roomNo, Integer editorUserNo, String title,
			LocalDateTime scheduledStartTime, LocalDateTime scheduledEndTime, List<Integer> participantUserNos) {
		if (roomNo == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomNo가 필요합니다.");
		if (scheduledStartTime == null || scheduledEndTime == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작/종료 시간이 필요합니다.");
		if (scheduledEndTime.isBefore(scheduledStartTime))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료가 시작보다 앞설 수 없습니다.");

		// 1) 방/생성자 확인 + 권한 체크(생성자만 수정)
		MeetingRoom room = meetingRoomDAO.findByRoomNo(roomNo);
		if (room == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회의를 찾을 수 없습니다.");

		Integer creatorUserNo = meetingRoomDAO.selectCreatorUserNo(roomNo);
		if (creatorUserNo == null || !creatorUserNo.equals(editorUserNo))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");

		// 2) 회의방 제목/시간 업데이트
		meetingRoomDAO.updateMeetingRoom(roomNo, title, scheduledStartTime, scheduledEndTime);

		// 3) 달력상세 동기화(존재 시)
		if (room.getCalDetailNo() != null) {
			LocalDate sDate = scheduledStartTime.toLocalDate();
			LocalTime sTime = scheduledStartTime.toLocalTime();
			LocalDate eDate = scheduledEndTime.toLocalDate();
			LocalTime eTime = scheduledEndTime.toLocalTime();
			meetingRoomDAO.updateCalendarDetail(room.getCalDetailNo(), title, sDate, sTime, eDate, eTime);
		}

		// 4) 참여자 동기화
		// - 회의참여자: 생성자(C001) 유지, 나머지 C002로 add/del
		// - 달력상세참여자: 생성자 제외 목록으로 맞춤
		LinkedHashSet<Integer> desired = new LinkedHashSet<>();
		if (participantUserNos != null)
			desired.addAll(participantUserNos);
		desired.add(creatorUserNo); // 방어적으로 생성자 포함

		List<Integer> currentRoom = meetingRoomDAO.selectParticipantUserNos(roomNo); // 생성자 포함
		// 회의 참가자 추가/삭제(생성자 제외)
		var toAddRoom = desired.stream().filter(u -> !currentRoom.contains(u)).filter(u -> !u.equals(creatorUserNo))
				.toList();
		var toDelRoom = currentRoom.stream().filter(u -> !desired.contains(u)).filter(u -> !u.equals(creatorUserNo))
				.toList();
		if (!toAddRoom.isEmpty())
			meetingRoomDAO.insertParticipants(roomNo, "C002", toAddRoom, null);
		if (!toDelRoom.isEmpty())
			meetingRoomDAO.deleteParticipants(roomNo, toDelRoom);

		// 달력상세 참가자 동기화(생성자 제외)
		if (room.getCalDetailNo() != null) {
			List<Integer> currentCal = meetingRoomDAO.selectCalendarDetailParticipantUserNos(room.getCalDetailNo());
			var desiredCal = desired.stream().filter(u -> !u.equals(creatorUserNo)).toList();

			var toAddCal = desiredCal.stream().filter(u -> !currentCal.contains(u)).toList();
			var toDelCal = currentCal.stream().filter(u -> !desiredCal.contains(u)).toList();

			if (!toAddCal.isEmpty())
				meetingRoomDAO.insertCalendarDetailParticipants(room.getCalDetailNo(), toAddCal);
			if (!toDelCal.isEmpty())
				meetingRoomDAO.deleteCalendarDetailParticipants(room.getCalDetailNo(), toDelCal);
		}
	}

	public void deleteRoomAndCalendar(Integer roomNo, Integer editorUserNo) {
		if (roomNo == null)
			return;

		MeetingRoom room = meetingRoomDAO.findByRoomNo(roomNo);
		if (room == null)
			return;

		Integer creatorUserNo = meetingRoomDAO.selectCreatorUserNo(roomNo);
		if (creatorUserNo == null || !creatorUserNo.equals(editorUserNo))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");

		// 순서: 달력상세참여자 → 달력상세 → 회의참여자 → 회의방
		if (room.getCalDetailNo() != null) {
			meetingRoomDAO.deleteCalendarDetailParticipantsAll(room.getCalDetailNo());
			meetingRoomDAO.deleteCalendarDetail(room.getCalDetailNo());
		}
		meetingRoomDAO.deleteParticipantsAll(roomNo);
		meetingRoomDAO.deleteMeetingRoom(roomNo);
	}

	public void logEnter(String roomCode, Integer userNo, String joinedAtIso) {
		var room = meetingRoomDAO.findByCode(roomCode);
		if (room == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회의방을 찾을 수 없습니다.");

		LocalDateTime joinedAt = parseIsoToLocal(joinedAtIso);

		int updated = meetingRoomDAO.updateJoinTime(room.getRoomNo(), userNo, joinedAt);
		if (updated == 0) {
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

	/* ---------------- 내부 유틸 ---------------- */

	private Integer getOrCreateTeamCalendar(Integer teamNo, Integer creatorUserNo) {
		Integer calNo = meetingRoomDAO.findCalNoByTeamNo(teamNo);
		if (calNo != null)
			return calNo;

		// 팀당 1개 달력 정책이라면 team_no UNIQUE 권장 → 경쟁 시 재조회
		try {
			meetingRoomDAO.insertTeamCalendar(teamNo, creatorUserNo);
			return meetingRoomDAO.selectLastInsertId(); // ← 방금 INSERT한 tb_calendar.cal_no
		} catch (DataIntegrityViolationException dup) {
			return meetingRoomDAO.findCalNoByTeamNo(teamNo);
		}
	}

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

	public RoomDetailRes getRoomDetail(String roomCode, Integer userNo) {
		var room = meetingRoomDAO.findByCode(roomCode);
		if (room == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회의방을 찾을 수 없습니다.");
		}

		// 참가자만 상세 확인 가능 (원하면 이 체크 제거 가능)
		String role = meetingRoomDAO.findRole(room.getRoomNo(), userNo);
		if (role == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
		}

		Integer creatorUserNo = meetingRoomDAO.selectCreatorUserNo(room.getRoomNo());
		var participants = meetingRoomDAO.selectParticipantsWithNames(room.getRoomNo());

		RoomDetailRes res = new RoomDetailRes();
		res.setRoomNo(room.getRoomNo());
		res.setRoomCode(room.getRoomCode());
		res.setTitle(room.getTitle());
		res.setScheduledTime(room.getScheduledTime());
		res.setScheduledEndTime(room.getScheduledEndTime());
		res.setCalDetailNo(/* meeting_room에 cal_detail_no 칼럼이 있다면 */ room.getCalDetailNo());
		res.setCreatorUserNo(creatorUserNo);
		res.setCreator(creatorUserNo != null && creatorUserNo.equals(userNo));
		res.setParticipants(participants);
		return res;
	}

	/* 응답 DTO (컨트롤러 호환 유지) */
	public record CreateResult(Integer roomNo, String roomCode, Integer calDetailNo) {
	}
}
