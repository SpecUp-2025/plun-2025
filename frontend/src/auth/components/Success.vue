<template>
    <div>
        <h2>성공입니당</h2>
        <h3>{{ email }}</h3>
        <h3>{{ name }} 환영합니다.</h3>
        <h3>{{ userNo }}</h3>
        
        <button @click="check" >토큰 전송 확인</button>
        <button @click ="logout">로그아웃</button>
    </div>
</template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed } from 'vue';
import { useRouter } from 'vue-router';

const userStore = useUserStore()
const router = useRouter()
const email = computed(() => userStore.user?.email ?? '');
const name = computed(() => userStore.user?.name ?? '');
const userNo = computed(() => userStore.user?.userNo ?? '');

const check = async () => {
    try {
        const res = await instance.get('/secure/ping')
        alert("요청성공 "  +res.status)
        console.log('[OK]', res.status)
    } catch (e) {
        alert('에러: ' + (e.response?.status ?? e.message)) // 401 나오면 토큰 문제
        console.log('[ERR]', e.response?.status, e.response?.data ?? e.message)
    }
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

<style lang="scss" scoped>

</style>