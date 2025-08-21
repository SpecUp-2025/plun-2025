package com.spec.plun.meeting.room;

import com.spec.plun.meeting.dto.MeetingCreateRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meeting-rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService service;
    
    @GetMapping("/active")  // ← 목록 핸들러 추가
    public ResponseEntity<List<MeetingRoom>> listActive() {
        return ResponseEntity.ok(service.listActive());
    }

    /** 회의방 생성 (MeetingCreateRequest 기반) */
    @PostMapping
    public ResponseEntity<CreateRes> create(@RequestBody MeetingCreateRequest req) {
        if (req.getScheduledEndTime() == null) { // ← 종료 예정시간 필수
            return ResponseEntity.badRequest().build();
        }

        MeetingRoom mr = new MeetingRoom();
        mr.setTeamNo(req.getTeamNo());
        mr.setTitle(req.getTitle());
        mr.setScheduledTime(req.getScheduledTime());      // null → XML NOW()
        mr.setScheduledEndTime(req.getScheduledEndTime());
        mr.setIsPrivate(Boolean.TRUE.equals(req.getPrivateRoom()) ? "Y" : "N");

        String plainPassword = req.getRoomPassword();
        Integer roomNo = service.create(mr, plainPassword);

        if (req.getParticipantIds() != null && !req.getParticipantIds().isEmpty()) {
            service.addParticipantsAllAsHost(roomNo, req.getParticipantIds());
        }

        return ResponseEntity.ok(new CreateRes(roomNo, mr.getRoomCode()));
    }

    public record CreateRes(Integer roomNo, String roomCode) {}
}
