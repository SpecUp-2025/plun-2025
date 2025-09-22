import { defineStore } from 'pinia'
import instance from '@/util/interceptors'

export const useAlarmStore = defineStore('alarm', {
  state: () => ({
    alarms: [],
    unreadCount: 0,
    loading: false,
  }),
  getters: {
    // 멘션 알림만 필터링
    mentionAlarms: (state) => {
      return state.alarms.filter(alarm => alarm.alarmType === 'CHAT_MENTION');
    },
    // 캘린더 초대 알림만 필터링 
    calendarInviteAlarms: (state) => {
      return state.alarms.filter(alarm => alarm.alarmType === 'CALENDAR_INVITE');
    },
    // 읽지 않은 멘션 알림 수
    unreadMentionCount: (state) => {
      return state.alarms.filter(alarm => 
        alarm.alarmType === 'CHAT_MENTION' && alarm.isRead === 'N'
      ).length;
    },
    // 일반 채팅 알림 수
    unreadChatCount: (state) => {
      return state.alarms.filter(alarm => 
        alarm.alarmType === 'CHAT' && alarm.isRead === 'N'
      ).length;
    },
    // 캘린더 초대 알림 수
    unreadCalendarInviteCount: (state) => {
      return state.alarms.filter(alarm => 
        alarm.alarmType === 'CALENDAR_INVITE' && alarm.isRead === 'N'
      ).length;
    }
  },
  actions: {
    async fetchAlarms(userNo) {
      if (!userNo) {
        console.error("userNo가 유효하지 않습니다.");
        return;
      }
      this.loading = true;
      try {
        const res = await instance.get(`/alarms/${userNo}`);
        this.alarms = res.data;
        this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length;
        console.log('알림 불러오기 성공:', res.data);
        console.log('읽지 않은 알림 수:', this.unreadCount);
      } catch (error) {
        console.error('알림 불러오기 실패:', error);
      } finally {
        this.loading = false;
      }
    },
    markAsRead(alarmNo) {
      const alarm = this.alarms.find(a => a.alarmNo === alarmNo);
      if (alarm && alarm.isRead === 'N') {
        alarm.isRead = 'Y';
        this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length;
        console.log('알림 읽음 처리:', alarmNo, '남은 읽지 않은 알림:', this.unreadCount);
        instance.put(`/alarms/${alarmNo}/read`)
          .then(() => {
            console.log('알림 읽음 처리 완료');
          })
          .catch((error) => {
            console.error('알림 읽음 처리 실패:', error);
          });
      }
    },
    addAlarm(alarm) {
      const referenceNo = alarm.referenceNo;
      // 중복 방지를 위해 동일한 alarmNo가 있는지 확인
      const existingAlarm = this.alarms.find(a => a.alarmNo === alarm.alarmNo);
      if (existingAlarm) {
        console.log('이미 존재하는 알림:', alarm.alarmNo);
        return;
      }

      const newAlarm = {
        alarmNo: alarm.alarmNo ?? Date.now(),
        alarmType: alarm.alarmType,
        referenceNo: referenceNo,
        content: alarm.content,
        isRead: alarm.isRead ?? 'N',
        senderName: alarm.senderName,
        eventTitle: alarm.eventTitle,
        eventStartTime: alarm.eventStartTime,
        teamNo: alarm.teamNo,
        inviterName: alarm.inviterName
      };
      
      this.alarms.unshift(newAlarm);
      this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length;
      console.log('새 알림 추가:', newAlarm, '총 읽지 않은 알림:', this.unreadCount);
      
      // 알림 타입별 로그
      if (alarm.alarmType === 'CHAT_MENTION') {
        console.log('새 멘션 알림 추가:', newAlarm);
      } else if (alarm.alarmType === 'CALENDAR_INVITE') {
        console.log('새 캘린더 초대 알림 추가:', newAlarm);
      }
    },

    // 알림 타입별 기본 메시지
    getDefaultMessage(alarmType) {
      const messageMap = {
        'CHAT': '새로운 메시지가 도착했습니다.',
        'CHAT_MENTION': '새로운 멘션이 도착했습니다.',
        'CALENDAR_INVITE': '새로운 일정에 초대되었습니다.'
      };
      return messageMap[alarmType] || '새로운 알림이 도착했습니다.';
    },
    async markAlarmsAsReadByRoom(roomNo) {
      try {
        const unreadAlarms = this.alarms.filter(a => a.referenceNo === roomNo && a.isRead === 'N');
        await Promise.all(
          unreadAlarms.map(a => instance.put(`/alarms/${a.alarmNo}/read`).catch((error) => console.error(`알림 ${a.alarmNo} 읽음 처리 실패:`, error)))
        );
        this.alarms = this.alarms.map(a => (a.referenceNo === roomNo ? { ...a, isRead: 'Y' } : a));
        this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length;
        console.log(`방 ${roomNo}의 알림 모두 읽음 처리 완료, 남은 읽지 않은 알림:`, this.unreadCount);
      } catch (error) {
        console.error('알림 읽음 처리 실패:', error);
      }
    },
    // 캘린더 초대 알림 생성
    createCalendarInviteAlarm(eventData, inviterName, invitedUserNo) {
      const inviteAlarm = {
        alarmNo: Date.now() + Math.random(),
        alarmType: 'CALENDAR_INVITE',
        referenceNo: eventData.calDetailNo,
        content: `${inviterName}님이 "${eventData.title}" 일정에 초대했습니다.`,
        isRead: 'N',
        eventTitle: eventData.title,
        eventStartTime: `${eventData.startDate}T${eventData.startTime}`,
        teamNo: eventData.teamNo,
        inviterName: inviterName,
        targetUserNo: invitedUserNo
      };
      return inviteAlarm;
    },
    reset() {
      this.alarms = [];
      this.unreadCount = 0;
      console.log('알림 스토어 초기화');
    }
  },
  persist: true,
})