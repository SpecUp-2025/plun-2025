<template>
  <header class="home-header">
    <div class="header">
      <div class="left">
        <RouterLink to="/teamList" class="logo"><img src = "@/assets/icons/PLUN.png" alt="로고"/></img></RouterLink>
      </div>
      
      <div class="right">
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
    </div>
  </header>
</template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()
const name = computed(() => userStore.user?.name ?? '')
const menuOpen = ref(false)
const userWrap =ref(false)

const handleClickOutside = (e) => {
  if (!menuOpen.value) return
  const el = userWrap.value
  if (el && !el.contains(e.target)) {
    menuOpen.value = false
  }
}

onMounted(()=>{
  document.addEventListener('click', handleClickOutside,true)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside,true)
})

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
</script>

<style scoped>
.home-header{
  position: fixed; top: 0; left: 0; right: 0;
  background: #fff;
  border-bottom: 1px solid #eef0f3;
  z-index: 50;
}
.header{
  height: 64px;                 
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0 16px;              
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo img{
  height: 40px;               
  width: auto; 
  display: block;
}

.right{ display:flex; align-items:center; gap: 8px; }
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
}
.menu > button{
  display: block; width: 100%; text-align: left;
  background: transparent; border: 0; cursor: pointer;
  padding: 8px 10px; border-radius: 8px; color: #111827; font-size: 14px;
}
.menu > button:hover{ background: #f3f4f6; }
</style>
