package com.spec.plun.meeting.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/meeting-rooms")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService service;

    /** 회의방 생성 */
    @PostMapping
    public ResponseEntity<CreateRes> create(@RequestBody CreateReq req) {
        MeetingRoom mr = new MeetingRoom();
        mr.setTeamNo(req.teamNo);                 // null 가능
        mr.setTitle(req.title);                   // 필수
        mr.setScheduledTime(req.scheduledTime);   // null이면 NOW 처리(XML에서)
        mr.setScheduledEndTime(req.scheduledEndTime);
        mr.setIsPrivate(req.isPrivate);           // null이면 Service에서 'N'
        mr.setRoomPasswordHash(req.roomPasswordHash);

        Integer roomNo = service.create(mr);
        return ResponseEntity.ok(new CreateRes(roomNo, mr.getRoomCode()));
    }

    /** 단건 조회 */
    @GetMapping("/{roomNo}")
    public ResponseEntity<MeetingRoom> get(@PathVariable Integer roomNo) {
        return ResponseEntity.ok(service.findById(roomNo));
    }
    
    @GetMapping("/{roomNo}/join-info")
    public ResponseEntity<JoinInfoRes> joinInfo(@PathVariable("roomNo") Integer roomNo) {
        MeetingRoom room = service.findById(roomNo);
        if (room == null) return ResponseEntity.notFound().build();
        String roomCode = room.getRoomCode();
        String joinUrl = "https://meet.jit.si/" + roomCode;
        return ResponseEntity.ok(new JoinInfoRes(roomCode, joinUrl));
    }

    public record JoinInfoRes(String roomCode, String joinUrl) {}

    // ---------- DTO ----------
    @Data
    public static class CreateReq {
        private Integer teamNo;
        @NotBlank @Size(max = 100)
        private String title;
        private LocalDateTime scheduledTime;        // ISO-8601 문자열로 받음 (예: "2025-08-16T10:00:00")
        private LocalDateTime scheduledEndTime;
        private String isPrivate;                   // 'Y' or 'N'
        private String roomPasswordHash;            // 추후 해시 적용 예정
    }

    public record CreateRes(Integer roomNo, String roomCode) {}
}
