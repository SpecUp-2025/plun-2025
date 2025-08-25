package com.spec.plun.meeting.room;

import com.spec.plun.meeting.dto.MeetingCreateRequest;
import com.spec.plun.meeting.dto.MeetingRoomListDto;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//package com.spec.plun.meeting.room;

@RestController
@RequestMapping("/meeting-rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

	private final MeetingRoomService service;

	@GetMapping("/active")
	public ResponseEntity<List<MeetingRoomListDto>> listActive(
			@RequestParam("teamNo") Integer teamNo,
			@RequestParam("userNo") Integer userNo) {
		return ResponseEntity.ok(service.listActiveByTeamAndMember(teamNo, userNo)
		);
	}

	@PostMapping
	public ResponseEntity<CreateRes> create(@RequestBody MeetingCreateRequest req) {
	    if (req.getScheduledEndTime() == null) {
	        return ResponseEntity.badRequest().build();
	    }
	    if (req.getCreatorUserNo() == null) {
	        return ResponseEntity.badRequest().build();
	    }

	    MeetingRoom mr = new MeetingRoom();
	    mr.setTeamNo(req.getTeamNo());
	    mr.setTitle(req.getTitle());
	    mr.setScheduledTime(req.getScheduledTime());
	    mr.setScheduledEndTime(req.getScheduledEndTime());
	    mr.setIsPrivate(Boolean.TRUE.equals(req.getPrivateRoom()) ? "Y" : "N");

	    List<Integer> allSelected = (req.getParticipantIds() == null) ? List.of() : req.getParticipantIds();

	    var result = service.createRoomAndParticipants(
	            mr,
	            req.getRoomPassword(),
	            req.getCreatorUserNo(),
	            allSelected
	    );

	    return ResponseEntity.ok(new CreateRes(result.roomNo(), result.roomCode()));
	}
	
	@GetMapping("/{roomCode}/authz")
    public ResponseEntity<AuthzRes> authorize(
            @PathVariable("roomCode") String roomCode,
            @RequestParam("userNo") Integer userNo
    ) {
        return ResponseEntity.ok(service.checkAuthz(roomCode, userNo));
    }

    /** 프리조인 권한 응답 DTO */
    public record AuthzRes(String title, String role, boolean authorized) {}
	public record CreateRes(Integer roomNo, String roomCode) {}
	
	
}
