package com.spec.plun.meeting.room;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spec.plun.meeting.dto.MeetingCreateRequest;
import com.spec.plun.meeting.dto.MeetingRoomListDto;
import com.spec.plun.meeting.dto.RoomDetailRes;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/meeting-rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

	private final MeetingRoomService service;

	@GetMapping("/active")
	public ResponseEntity<List<MeetingRoomListDto>> listActive(@RequestParam("teamNo") Integer teamNo,
			@RequestParam("userNo") Integer userNo) {
		return ResponseEntity.ok(service.listActiveByTeamAndMember(teamNo, userNo));
	}

	@PostMapping
	public ResponseEntity<CreateRes> create(@RequestBody MeetingCreateRequest req) {
		if (req.getCreatorUserNo() == null) {
			return ResponseEntity.badRequest().body(CreateRes.error("creatorUserNo가 필요합니다."));
		}
		if (req.getScheduledTime() == null || req.getScheduledEndTime() == null) {
			return ResponseEntity.badRequest().body(CreateRes.error("시작/종료 시간이 필요합니다."));
		}
		if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
			return ResponseEntity.badRequest().body(CreateRes.error("제목이 필요합니다."));
		}
		if (req.getScheduledEndTime().isBefore(req.getScheduledTime())) {
			return ResponseEntity.badRequest().body(CreateRes.error("종료가 시작보다 앞설 수 없습니다."));
		}

		MeetingRoom mr = new MeetingRoom();
		mr.setTeamNo(req.getTeamNo());
		mr.setTitle(req.getTitle().trim());
		mr.setScheduledTime(req.getScheduledTime());
		mr.setScheduledEndTime(req.getScheduledEndTime());

		List<Integer> raw = (req.getParticipantIds() == null) ? List.of() : req.getParticipantIds();
		LinkedHashSet<Integer> uniq = new LinkedHashSet<>(raw);
		uniq.add(req.getCreatorUserNo());
		List<Integer> participantIds = List.copyOf(uniq);

		var result = service.createRoomWithCalendar(mr, req.getCreatorUserNo(), participantIds);
		return ResponseEntity.ok(CreateRes.ok(result.roomNo(), result.roomCode(), result.calDetailNo()));
	}

	@GetMapping("/{roomCode}/authz")
	public ResponseEntity<AuthzRes> authorize(@PathVariable("roomCode") String roomCode,
			@RequestParam("userNo") Integer userNo) {
		return ResponseEntity.ok(service.checkAuthz(roomCode, userNo));
	}

	@PostMapping("/{roomCode}/enter")
	public ResponseEntity<Void> enter(@PathVariable("roomCode") String roomCode, @RequestBody EnterLeaveReq req) {
		service.logEnter(roomCode, req.userNo(), req.joinedAt());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{roomCode}/leave")
	public ResponseEntity<Void> leave(@PathVariable("roomCode") String roomCode, @RequestBody EnterLeaveReq req) {
		service.logLeave(roomCode, req.userNo(), req.joinedAt(), req.leftAt());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{roomCode}")
	public ResponseEntity<RoomDetailRes> getDetail(@PathVariable("roomCode") String roomCode,
			@RequestParam("userNo") Integer userNo) {
		return ResponseEntity.ok(service.getRoomDetail(roomCode, userNo));
	}

	/* ---------- ⬇️ 신규: 수정 & 삭제 ---------- */

	@PatchMapping("/{roomNo}")
	public ResponseEntity<UpdateRes> update(@PathVariable("roomNo") Integer roomNo,
			@RequestParam("userNo") Integer editorUserNo, // 생성자만 허용
			@RequestBody MeetingUpdateRequest req) {

		// 입력 유효성
		if (req.title() != null && req.title().trim().isEmpty()) {
			return ResponseEntity.badRequest().body(UpdateRes.error("제목이 비어 있습니다."));
		}
		if (req.scheduledStartTime() == null || req.scheduledEndTime() == null) {
			return ResponseEntity.badRequest().body(UpdateRes.error("시작/종료 시간이 필요합니다."));
		}
		if (req.scheduledEndTime().isBefore(req.scheduledStartTime())) {
			return ResponseEntity.badRequest().body(UpdateRes.error("종료가 시작보다 앞설 수 없습니다."));
		}

		service.updateRoomAndCalendar(roomNo, editorUserNo, req.title(), req.scheduledStartTime(),
				req.scheduledEndTime(), req.participantUserNos());

		return ResponseEntity.ok(UpdateRes.ok(roomNo));
	}

	@DeleteMapping("/{roomNo}")
	public ResponseEntity<Void> delete(@PathVariable("roomNo") Integer roomNo,
			@RequestParam("userNo") Integer editorUserNo) {
		service.deleteRoomAndCalendar(roomNo, editorUserNo);
		return ResponseEntity.noContent().build();
	}

	/* ---------- DTOs ---------- */

	public static record AuthzRes(String title, String role, boolean authorized) {
	}

	public static record CreateRes(Integer roomNo, String roomCode, Integer calDetailNo, String message) {
		public static CreateRes ok(Integer roomNo, String roomCode, Integer calDetailNo) {
			return new CreateRes(roomNo, roomCode, calDetailNo, null);
		}

		public static CreateRes error(String message) {
			return new CreateRes(null, null, null, message);
		}
	}

	/** 수정 요청 DTO (프론트 계약에 맞춤: 'YYYY-MM-DDTHH:mm' 로컬 문자열 -> LocalDateTime) */
	public static record MeetingUpdateRequest(String title, LocalDateTime scheduledStartTime,
			LocalDateTime scheduledEndTime, List<Integer> participantUserNos) {
	}

	public static record UpdateRes(Integer roomNo, String message) {
		public static UpdateRes ok(Integer roomNo) {
			return new UpdateRes(roomNo, null);
		}

		public static UpdateRes error(String message) {
			return new UpdateRes(null, message);
		}
	}

	public static record EnterLeaveReq(Integer userNo, String joinedAt, String leftAt) {
	}
}
