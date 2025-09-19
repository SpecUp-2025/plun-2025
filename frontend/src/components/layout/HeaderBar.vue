<template>
  <link
    rel="stylesheet"
    href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css"
    />
  <header class="header-bar">
    <div class="logo">PLUN</div>

    <div class="page-title">
      <slot>페이지 제목</slot>
    </div>

    <div class="header-actions">
      <button class="invite-button" @click="router.push({name:'teamInvite'})">+</button>
      <AlarmDropdown @alarmClicked="$emit('alarmClicked', $event)" />&nbsp;
      <div class="user-wrap" ref="userWrap" >
          <button class="user-trigger" @click="menuOpen = !menuOpen">
            <span class="name">{{ name }}</span>
          </button>
          <div v-if="menuOpen" class="menu">
            <button @click="goAccount">계정 설정</button>
            <button @click="logout">로그아웃</button>
          </div>
        </div>
    </div>
  </header>
</template>

<script setup>
import { useAlarmStore } from '@/store/useAlarmStore'
import instance from '@/util/interceptors';
import AlarmDropdown from '@/components/alarm/AlarmDropdown.vue'
import { useUserStore } from '@/store/userStore'
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router';

defineEmits([ 'alarmClicked'])
const router = useRouter()
const userStore = useUserStore()
const name = computed(() => userStore.user?.name ?? '')
const menuOpen = ref(false)
const alarmStore = useAlarmStore()
const userWrap =ref(false)

const handleClickOutside = (e) => {
  if (!menuOpen.value) return
  const el = userWrap.value
  if (el && !el.contains(e.target)) {
    menuOpen.value = false
  }
}
const goAccount = () => {
  menuOpen.value = false
  router.push({ name: 'detail' })
}
const logout = async () => {
  try {
    const {status} = await instance.post('/auth/logout',{
        refreshToken : localStorage.getItem('refreshToken')
    })
    if(status==200){
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      userStore.removeUser()
      router.push({ name: 'login' })
    }
  } catch (error) {
    console.error("로그아웃 실패",error)
  }
}

onMounted(()=>{
  document.addEventListener('click', handleClickOutside,true)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside,true)
})

</script>

<style scoped>
.user-wrap{ position: relative; }
.user-trigger{
  display: inline-flex; align-items: center; gap: 8px;
  background: transparent; border: 0; cursor: pointer;
  padding: 6px 10px; border-radius: 10px; color: #374151;
  font-size: 20px;
}
.user-trigger:hover{ background: #f3f4f6; }

.menu{
  position: absolute; right: 0; top: calc(100% + 8px);
  background: #fff; border: 1px solid #e5e7eb; border-radius: 12px;
  padding: 6px; box-shadow: 0 12px 30px rgba(0,0,0,.08);
  width: 160px;
  z-index: 10010;
}
.menu > button{
  display: block; width: 100%; text-align: left;
  background: transparent; border: 0; cursor: pointer;
  padding: 8px 10px; border-radius: 8px; color: #111827; font-size: 14px;
}
.menu > button:hover{ background: #f3f4f6; }

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  padding: 12px 16px;
  border-bottom: 2px solid #007BFF;
}

.logo {
  font-weight: bold;
  font-size: 18px;
  color: #007BFF;
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

.invite-button{
  --btn-size: 34px;
  inline-size: var(--btn-size);
  block-size: var(--btn-size);
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 10px;
  background: #3399FF;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(51,153,255,.35);
  transition: background .15s ease, box-shadow .15s ease, transform .05s ease;
}
.invite-button:hover{ background:#1e80ff; }
.invite-button:active{ transform: translateY(1px); box-shadow: 0 1px 4px rgba(51,153,255,.3); }
.invite-button:focus-visible{ outline: 2px solid #93c5fd; outline-offset: 2px; }

.header-actions{ display:flex; align-items:center; gap:12px; }

</style>
