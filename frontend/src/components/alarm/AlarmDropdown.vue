<template>
  <div class="alarm-dropdown">
    <div class="alarm-icon" @click="toggleDropdown">
      🔔
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
    </div>

    <div v-if="showDropdown" class="dropdown-content">
      <div v-if="alarms.length === 0" class="no-alarm">알림이 없습니다</div>

      <ul v-else>
        <li
          v-for="alarm in alarms"
          :key="alarm.alarmNo"
          @click="goToChatRoom(alarm)"
          class="alarm-item"
        >
          <strong>{{ alarm.senderName }}</strong> : {{ alarm.content }}
        </li>
      </ul>

      <button v-if="alarms.length" @click="markAllAsRead">모두 읽음</button>
    </div>
  </div>
</template>

<script>
import instance from '@/util/interceptors'

export default {
  name: "AlarmDropdown",
  props: {
    alarms: Array, // 부모 컴포넌트에서 받은 알림 배열
  },
  data() {
    return {
      showDropdown: false,
    };
  },
  computed: {
    unreadCount() {
      return this.alarms.filter(alarm => alarm.isRead === 'N').length;
    },
  },
  methods: {
    toggleDropdown() {
      this.showDropdown = !this.showDropdown;
    },
    async goToChatRoom(alarm) {
      try {
        // 읽음 처리
        await instance.put(`/alarms/${alarm.alarmNo}/read`);
        alarm.isRead = 'Y';

        // 채팅방으로 이동
        this.$router.push(`/chat/${alarm.referenceNo}`);
        this.showDropdown = false;
      } catch (error) {
        console.error('❌ 알림 읽음 처리 실패', error);
      }
    },
    async markAllAsRead() {
      const unreadAlarms = this.alarms.filter(a => a.isRead === 'N');
      for (const alarm of unreadAlarms) {
        try {
          await instance.put(`/alarms/${alarm.alarmNo}/read`);
          alarm.isRead = 'Y';
        } catch (e) {
          console.error("❌ 알림 읽음 처리 실패", e);
        }
      }
    },
  },
};
</script>

<style scoped>
.alarm-dropdown {
  position: relative;
  display: inline-block;
  margin-left: 20px;
}
.badge {
  background-color: #4285F4; /* 파란색 */
  color: white;
  border-radius: 9999px;
  font-size: 12px;
  padding: 2px 6px;
  position: absolute;
  top: -5px;
  right: -10px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  font-weight: bold;
  box-shadow: 0 0 0 2px white; /* 흰 테두리로 더 눈에 띄게 */
}
.alarm-icon {
  cursor: pointer;
  position: relative;
}
.dropdown-content {
  position: absolute;
  background-color: white;
  border: 1px solid #ccc;
  width: 250px;
  right: 0;
  margin-top: 10px;
  z-index: 100;
  padding: 10px;
}
.alarm-item {
  padding: 8px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}
.alarm-item:hover {
  background-color: #f5f5f5;
}
.no-alarm {
  text-align: center;
  padding: 10px;
  color: gray;
}
</style>
