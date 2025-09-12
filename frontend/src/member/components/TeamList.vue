<template>
    <div>
        <h3>{{ name }} 환영합니다.</h3>
        <h3 >{{ email }}</h3> 
        <button @click="router.push({name : 'detail'})">계정 설정</button>
        <button @click="router.push({name:'invitation'})">초대보관함</button>
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