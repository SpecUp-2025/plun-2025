import { defineStore } from 'pinia'
import instance from '@/util/interceptors'

export const useAlarmStore = defineStore('alarm', {
  state: () => ({
    alarms: [],
    loading: false,
  }),
  getters: {

    // 모든 알림 중 읽지 않은 개수
    unreadCount: (state) => {
      return state.alarms.filter(alarm => alarm.isRead === 'N').length;
    },
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
    },
    // 캘린더 수정 알림 수
    unreadCalendarUpdateCount: (state) => {
      return state.alarms.filter(alarm => 
        alarm.alarmType === 'CALENDAR_UPDATE' && alarm.isRead === 'N'
      ).length;
    },
    // 캘린더 삭제 알림 수  
    unreadCalendarDeleteCount: (state) => {
      return state.alarms.filter(alarm => 
        alarm.alarmType === 'CALENDAR_DELETE' && alarm.isRead === 'N'
      ).length;
    }
  },
  actions: {
    async fetchAlarms(userNo) {
      if (!userNo) {
        console.error("⛔ userNo가 유효하지 않습니다.");
        return;
      }

      this.loading = true;
      try {
        const res = await instance.get(`/alarms/${userNo}`);
        this.alarms = res.data;
        console.log('✅ 알림 불러오기 성공:', res.data);
      } catch (error) {
        console.error('❌ 알림 불러오기 실패:', error);
      } finally {
        this.loading = false;
      }
    },

    markAsRead(alarmNo) {
      const alarm = this.alarms.find(a => a.alarmNo === alarmNo);

      if (alarm && alarm.isRead === 'N') {
        alarm.isRead = 'Y';
        instance.put(`/alarms/${alarmNo}/read`)
          .then(() => {
            console.log(`✅ 알림 ${alarmNo} 읽음 처리 완료`);
          })
          .catch((error) => {
            console.error(`❌ 알림 ${alarmNo} 읽음 처리 실패:`, error);
          });
      }
    },

    addAlarm(alarm) {
      if (!alarm?.alarmNo) {
        console.warn('⛔ 잘못된 알림 데이터, 추가 중단:', alarm);
        return;
      }

      const exists = this.alarms.find(a => a.alarmNo === alarm.alarmNo);
      if (exists) {
        console.log('⚠️ 이미 존재하는 알림:', alarm.alarmNo);
        return;
      }

      const newAlarm = {
        alarmNo: alarm.alarmNo,
        alarmType: alarm.alarmType,
        referenceNo: alarm.referenceNo,
        content: alarm.content ?? this.getDefaultMessage(alarm.alarmType),
        isRead: alarm.isRead ?? 'N',
        senderName: alarm.senderName,
        eventTitle: alarm.eventTitle,
        eventStartTime: alarm.eventStartTime,
        teamNo: alarm.teamNo,
        inviterName: alarm.inviterName,
        userNo: alarm.userNo,
      };

      this.alarms.unshift(newAlarm);
      console.log(' 새 알림 추가:', newAlarm);
    },

    getDefaultMessage(alarmType) {
      const messageMap = {
        'CHAT': '새로운 메시지가 도착했습니다.',
        'CHAT_MENTION': '새로운 멘션이 도착했습니다.',
        'CALENDAR_INVITE': '새로운 일정에 초대되었습니다.',
        'CALENDAR_UPDATE': '일정이 수정되었습니다.',
        'CALENDAR_DELETE': '일정이 삭제되었습니다.'
      };
      return messageMap[alarmType] || '새로운 알림이 도착했습니다.';
    },

    async markAlarmsAsReadByRoom(roomNo) {
      const unreadAlarms = this.alarms.filter(a => a.referenceNo === roomNo && a.isRead === 'N');

      try {
        await Promise.all(
          unreadAlarms.map(a =>
            instance.put(`/alarms/${a.alarmNo}/read`)
              .catch(err => console.error(`❌ 알림 ${a.alarmNo} 읽음 처리 실패`, err))
          )
        );

        this.alarms = this.alarms.map(a =>
          a.referenceNo === roomNo ? { ...a, isRead: 'Y' } : a
        );

        console.log(` 방 ${roomNo}의 알림 모두 읽음 처리 완료`);
      } catch (error) {
        console.error('❌ 알림 일괄 읽음 처리 실패:', error);
      }
    },

    createCalendarInviteAlarm(eventData, inviterName, invitedUserNo) {
      const inviteAlarm = {
        alarmType: 'CALENDAR_INVITE',
        referenceNo: eventData.calDetailNo,
        content: `${inviterName}님이 "${eventData.title}" 일정에 초대했습니다.`,
        isRead: 'N',
        eventTitle: eventData.title,
        eventStartTime: `${eventData.startDate}T${eventData.startTime}`,
        teamNo: eventData.teamNo,
        inviterName: inviterName,
        userNo: invitedUserNo
      };
      return inviteAlarm;
    },

    reset() {
      this.alarms = [];
      console.log('알림 스토어 초기화');
    }
  },
  persist: true,
})