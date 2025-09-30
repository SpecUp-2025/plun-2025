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
    public void insertAlarm(Alarm alarm) {
        alarmDAO.insertAlarm(alarm);
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
		
		messagingTemplate.convertAndSend("/topic/notifications/" + userNo, alarm);
		System.out.println("[AlarmService] 캘린더 초대 알림 전송 완료 - userNo: " + userNo);
	}
    public String getUserNameByUserNo(Integer userNo) {
        return alarmDAO.selectUserNameByUserNo(userNo);
    }
    public void createCalendarAlarm(String type, Integer calDetailNo, String title, Integer teamNo, String senderName, Integer senderNo) {
        List<Integer> userNos = alarmDAO.selectUserNosByTeamNo(teamNo);
        for (Integer userNo : userNos) {
            Alarm alarm = new Alarm();
            alarm.setAlarmType(type);
            alarm.setReferenceNo(calDetailNo);
            alarm.setContent(title != null ? title : "일정 알림");
            alarm.setIsRead("N");
            alarm.setSenderName(senderName);
            alarm.setUserNo(userNo);
            
            alarm.setSenderNo(senderNo);

            alarmDAO.insertAlarm(alarm);
            System.out.println("[AlarmService] DB 저장 후 alarmNo: " + alarm.getAlarmNo());
            System.out.println("💡 생성된 alarmNo: " + alarm.getAlarmNo());
            messagingTemplate.convertAndSend("/topic/notifications/" + userNo, alarm);
        }
    }
}