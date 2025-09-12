package com.spec.plun.meeting.room;

import java.util.LinkedHashSet;
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
        // 0) 기본 유효성
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

        // 1) MeetingRoom 엔티티 채우기
        MeetingRoom mr = new MeetingRoom();
        mr.setTeamNo(req.getTeamNo());
        mr.setTitle(req.getTitle().trim());
        mr.setScheduledTime(req.getScheduledTime());
        mr.setScheduledEndTime(req.getScheduledEndTime());
        // 비공개/비밀번호 제거 → 관련 필드 설정 없음

        // 2) 참여자 집합(중복 제거 + 생성자 강제 포함)
        List<Integer> raw = (req.getParticipantIds() == null) ? List.of() : req.getParticipantIds();
        LinkedHashSet<Integer> uniq = new LinkedHashSet<>(raw);
        uniq.add(req.getCreatorUserNo()); // 서버에서도 보수적으로 보장
        List<Integer> participantIds = List.copyOf(uniq);

        // 3) 서비스 호출 (회의방 + 팀 달력상세 + 달력참여자까지 한 트랜잭션)
        // 비밀번호 파라미터 제거된 시그니처로 호출
        var result = service.createRoomWithCalendar(mr, req.getCreatorUserNo(), participantIds);

        // 4) 응답 (프론트가 calDetailNo 안 써도 무방)
        return ResponseEntity.ok(CreateRes.ok(result.roomNo(), result.roomCode(), result.calDetailNo()));
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
    public static record AuthzRes(String title, String role, boolean authorized) {
    }

    /** 생성 응답 DTO */
    public static record CreateRes(Integer roomNo, String roomCode, Integer calDetailNo, String message) {
        public static CreateRes ok(Integer roomNo, String roomCode, Integer calDetailNo) {
            return new CreateRes(roomNo, roomCode, calDetailNo, null);
        }

        public static CreateRes error(String message) {
            return new CreateRes(null, null, null, message);
        }
    }

    /** 입퇴장 요청 DTO */
    public static record EnterLeaveReq(Integer userNo, String joinedAt, String leftAt) {
    }
}
