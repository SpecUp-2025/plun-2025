package com.spec.plun.alarm.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.spec.plun.alarm.dao.AlarmDAO;
import com.spec.plun.alarm.entity.Alarm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlarmService {
	
	private final AlarmDAO alarmDAO;
	private final SimpMessagingTemplate messagingTemplate;
	
	public void createChatAlarm(int fromUserNo ,int toUserNo, int roomNo, String content) {
        Alarm alarm = new Alarm();
        alarm.setUserNo(toUserNo);
        alarm.setSenderNo(fromUserNo);
        alarm.setAlarmType("CHAT");
        alarm.setReferenceNo(roomNo);
        alarm.setContent(content);
        alarm.setIsRead("N");
        
        // 사용자 이름 조회 (DAO 메서드 필요)
        String senderName = alarmDAO.selectUserNameByUserNo(fromUserNo);
        System.out.println("[AlarmService] 사용자 이름: " + senderName);
        alarm.setSenderName(senderName);

        alarmDAO.insertAlarm(alarm);

        // 실시간 WebSocket 알림 전송
        System.out.println("[AlarmService] 알림 전송 전: " + alarm);
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + toUserNo,
            alarm
        );
    }

    public List<Alarm> getUserAlarms(int userNo) {
        return alarmDAO.selectAlarmsByUserNo(userNo);
    }

    public void markAsRead(int alarmNo) {
    	alarmDAO.updateAlarmIsRead(alarmNo);
    }
}
