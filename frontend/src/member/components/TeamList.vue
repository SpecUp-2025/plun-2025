<template>
    <div>
        <h3>{{ name }} 환영합니다.</h3>
        <h3 >{{ email }}</h3> 
        <button @click="router.push({name : 'detail'})">계정 설정</button>
        
        <h1>팀 리스트</h1>

        <div v-if="!team.list.length">
          팀 리스트가 없습니다.
        </div>
        <ul v-else>
          <li v-for="(item,index) in team.list" :key="item.teamNo">
            {{ index }}
            {{ item.teamNo }}
            {{ item.teamName }}
            {{ item.createDate }}
            <button @click = "router.push({name : 'teamMain', params:{ teamNo: item.teamNo }})">입장하기</button>
            <button @click = "router.push({name:'setting',params:{teamNo:item.teamNo}})">설정하기</button>
          </li>
        </ul>
        <button @click="router.push({name:'teamCreate'})">팀 추가하기</button>
        <button @click="check" >토큰 전송 확인</button>
        <button @click ="logout">로그아웃</button>

        <RouterView/>
    </div>
</template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive } from 'vue';
import { RouterView, useRouter } from 'vue-router';

const userStore = useUserStore()
const router = useRouter()
const email = computed(() => userStore.user?.email ?? '');
const name = computed(() => userStore.user?.name ?? '');
const userNo = computed(() => userStore.user?.userNo ?? '');
const team = reactive({
  list:[],
})

onMounted(async ()=>{
  await teamList()
}) 


const teamList = async ()=>{
  try {
    const {status , data} = await instance.get(`/teams/teamList/${userNo.value}`)
    if(status===200){
      team.list = data
    }
  } catch (error) {
    console.error("리스트를 불러오는데 실패했습니다.",error)
  }
}

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