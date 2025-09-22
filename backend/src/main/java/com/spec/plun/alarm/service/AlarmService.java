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

    public void createChatAlarm(Integer senderNo, Integer userNo, Integer roomNo, String content) {
        Alarm alarm = new Alarm();
        alarm.setUserNo(userNo);
        alarm.setSenderNo(senderNo);
        alarm.setAlarmType("CHAT");
        alarm.setReferenceNo(roomNo);
        alarm.setContent(content);
        alarm.setIsRead("N");

        String senderName = alarmDAO.selectUserNameByUserNo(senderNo);
        System.out.println("[AlarmService] 사용자 이름: " + senderName);
        alarm.setSenderName(senderName);

        alarmDAO.insertAlarm(alarm);

        System.out.println("[AlarmService] 알림 전송 전: " + alarm);
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + userNo,
            alarm
        );
    }
    public List<Alarm> getUserAlarms(Integer userNo) {
        return alarmDAO.selectAlarmsByUserNo(userNo);
    }
    public void markAsRead(Integer alarmNo) {
        alarmDAO.updateAlarmIsRead(alarmNo);
    }
    public void createMentionAlarm(Integer senderNo, Integer userNo, Integer roomNo, String content) {
        Alarm alarm = new Alarm();
        alarm.setUserNo(userNo);
        alarm.setSenderNo(senderNo);
        alarm.setAlarmType("CHAT_MENTION");
        alarm.setReferenceNo(roomNo);
        alarm.setContent(content);
        alarm.setIsRead("N");

        String senderName = alarmDAO.selectUserNameByUserNo(senderNo);
        alarm.setSenderName(senderName);

        alarmDAO.insertAlarm(alarm);

        messagingTemplate.convertAndSend("/topic/notifications/" + userNo, alarm);
    }
    public void createCalendarInviteAlarm(Integer senderNo, Integer userNo, 
            Integer calDetailNo, String content, 
            String eventStartTime, Integer teamNo) {

		System.out.println("[AlarmService] 캘린더 초대 알림 생성 시작");
		System.out.println("  - inviterNo: " + senderNo);
		System.out.println("  - invitedUserNo: " + userNo);
		System.out.println("  - calDetailNo: " + calDetailNo);
		System.out.println("  - content: " + content);
		
		Alarm alarm = new Alarm();
		alarm.setUserNo(userNo);
		alarm.setSenderNo(senderNo);
		alarm.setAlarmType("CALENDAR_INVITE");
		alarm.setReferenceNo(calDetailNo);
		alarm.setIsRead("N");
		
		// 초대한 사용자 이름 조회
		String inviterName = alarmDAO.selectUserNameByUserNo(senderNo);
		alarm.setSenderName(inviterName);
		
		// 초대한 사용자 이름 조회
		String invitedName = alarmDAO.selectUserNameByUserNo(userNo);
		alarm.setName(invitedName);
		
		
		// 알림 내용 생성
		String calContent1 = inviterName + "님이 \"" + invitedName + "\"을 일정에 초대했습니다.";
		alarm.setContent(calContent1);
		
		System.out.println("[AlarmService] 캘린더 초대 알림 생성: " + alarm);
		alarmDAO.insertAlarm(alarm);
		
		// WebSocket으로 실시간 전송
		messagingTemplate.convertAndSend("/topic/notifications/" + userNo, alarm);
		System.out.println("[AlarmService] 캘린더 초대 알림 전송 완료 - userNo: " + userNo);
	}
}