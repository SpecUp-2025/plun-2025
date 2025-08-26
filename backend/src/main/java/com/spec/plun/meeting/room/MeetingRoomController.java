package com.spec.plun.meeting.room;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.meeting.dto.MeetingCreateRequest;
import com.spec.plun.meeting.dto.MeetingRoomListDto;

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
		if (req.getScheduledEndTime() == null)
			return ResponseEntity.badRequest().build();
		if (req.getCreatorUserNo() == null)
			return ResponseEntity.badRequest().build();

		MeetingRoom mr = new MeetingRoom();
		mr.setTeamNo(req.getTeamNo());
		mr.setTitle(req.getTitle());
		mr.setScheduledTime(req.getScheduledTime());
		mr.setScheduledEndTime(req.getScheduledEndTime());
		mr.setIsPrivate(Boolean.TRUE.equals(req.getPrivateRoom()) ? "Y" : "N");

		List<Integer> allSelected = (req.getParticipantIds() == null) ? List.of() : req.getParticipantIds();

		var result = service.createRoomAndParticipants(mr, req.getRoomPassword(), req.getCreatorUserNo(), allSelected);

		return ResponseEntity.ok(new CreateRes(result.roomNo(), result.roomCode()));
	}

	@GetMapping("/{roomCode}/authz")
	public ResponseEntity<AuthzRes> authorize(@PathVariable("roomCode") String roomCode,
			@RequestParam("userNo") Integer userNo) {
		return ResponseEntity.ok(service.checkAuthz(roomCode, userNo));
	}

	// 입장
	@PostMapping("/{roomCode}/enter")
	public ResponseEntity<Void> enter(@PathVariable("roomCode") String roomCode, @RequestBody EnterLeaveReq req) {
		service.logEnter(roomCode, req.userNo(), req.joinedAt());
		return ResponseEntity.ok().build();
	}

	// 퇴장
	@PostMapping("/{roomCode}/leave")
	public ResponseEntity<Void> leave(@PathVariable("roomCode") String roomCode, @RequestBody EnterLeaveReq req) {
		service.logLeave(roomCode, req.userNo(), req.joinedAt(), req.leftAt());
		return ResponseEntity.ok().build();
	}

	/** 프리조인 권한 응답 DTO */
	public record AuthzRes(String title, String role, boolean authorized) {
	}

	public record CreateRes(Integer roomNo, String roomCode) {
	}

	// 프론트에서 넘길 바디: userNo, joinedAt/leftAt(ISO 문자열)
	public record EnterLeaveReq(Integer userNo, String joinedAt, String leftAt) {
	}
}
