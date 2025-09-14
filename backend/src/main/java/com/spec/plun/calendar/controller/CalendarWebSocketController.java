package com.spec.plun.calendar.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarWebSocketController {
	
	private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/calendar/refresh")
    public void handleCalendarRefresh(String message) {
        log.info("📩 [WebSocket] 메시지 수신됨: {}", message);

        // 정 userNo만 타게팅해야 함
        for (int userNo = 1; userNo <= 3; userNo++) {
            messagingTemplate.convertAndSend(
                "/topic/calendar/refresh/" + userNo,
                message
            );
        }
    }

}
