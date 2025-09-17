// src/store/useAlarmStore.js
import { defineStore } from 'pinia'
import instance from '@/util/interceptors' // axios 인스턴스 등 필요하면 import

export const useAlarmStore = defineStore('alarm', {
  state: () => ({
    alarms: [],
    unreadCount: 0,
  }),
  actions: {
    async fetchAlarms(userNo) {
      try {
        const res = await instance.get(`/alarms/${userNo}`)
        this.alarms = res.data
        this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length
      } catch (error) {
        console.error('알림 불러오기 실패:', error)
      }
    },
    markAsRead(alarmNo) {
      const alarm = this.alarms.find(a => a.alarmNo === alarmNo)
      if (alarm && alarm.isRead === 'N') {
        alarm.isRead = 'Y'
        this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length
        // 서버에도 업데이트 요청 추가 가능
        instance.put(`/alarms/${alarmNo}/read`).catch(console.error)
      }
    },
    addAlarm(alarm) {
      const roomNo = alarm.referenceNo
      console.log('[알림] 들어온 referenceNo:', roomNo)

      // 중복 제거: 같은 referenceNo의 알림 제거
      this.alarms = this.alarms.filter(a => a.referenceNo !== roomNo)

      const newAlarm = {
        alarmNo: alarm.alarmNo ?? Date.now(), // alarmNo 없을 경우 대비
        referenceNo: roomNo,
        content: '새로운 메시지가 도착했습니다.',
        isRead: 'N'
      }

      this.alarms.unshift(newAlarm)
      this.unreadCount = this.alarms.filter(a => a.isRead === 'N').length
    },
    reset() {
      this.alarms = []
      this.unreadCount = 0
    }
  },
  persist: true,  // pinia-plugin-persistedstate 플러그인 활용
})
