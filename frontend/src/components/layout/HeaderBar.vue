<template>
  <link
    rel="stylesheet"
    href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css"
    />
  <header class="header-bar">
    <RouterLink to="/teamList" class="logo"><img src = "@/assets/icons/PLUN.png" alt="로고"/></img></RouterLink>
    <div class="page-title">
      <slot>페이지 제목</slot>
    </div>

    <div class="header-actions">
      <button class="icon-button" @click="$emit('go-profile')"><i class="fas fa-user"></i></button>
      <AlarmDropdown @alarmClicked="$emit('alarmClicked', $event)" />&nbsp;
      <button class="logout-button" @click="$emit('logout')">로그아웃</button>
    </div>
  </header>
</template>

<script setup>
import { useAlarmStore } from '@/store/useAlarmStore'
import AlarmDropdown from '@/components/alarm/AlarmDropdown.vue'

defineEmits(['go-profile', 'alarmClicked', 'logout'])

const alarmStore = useAlarmStore()

</script>

<style scoped>

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  padding: 12px 16px;
  border-bottom: 2px solid #007BFF;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-button {
  background: none;
  border: none;
  font-size: 18px;
  color: #3399FF;
  cursor: pointer;
}

.icon-button:hover {
  color: #0056b3;
}

.logout-button {
  background-color: #3399FF;
  color: #fff;
  border: none;
  padding: 6px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.logout-button:hover {
  background-color: #0056b3;
}

.logo img{
  height: 40px;               
  width: auto; 
  display: block;
}
</style>
