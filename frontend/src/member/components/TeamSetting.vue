<template>
    <div>
        {{form.list.teamName}}
         <button @click = "router.push({name : 'teamMain', params:{ teamNo: item.teamNo }})">입장하기</button>
        {{ form.list.createDate }}
        <button @click="teamDelete()">탈퇴하기</button>
        <button @click="router.back()">뒤로가기</button>
    </div>
</template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
const form = reactive({
    list:{}
})
const userStore = useUserStore()
const userNo = computed(() => userStore.user?.userNo ?? '')
onMounted( async () => {
    await teamDetail()
})

const teamDetail = async () => {
    try {
        const {status,data} = await instance.get(`/teams/teamDetail/${route.params.teamNo}`)
        if(status ===200)    {
            form.list = data.list
        }
    } catch (error) {
        console.error("팀 정보 확인 실패",error)
    }
    
}

const teamDelete = async () => {
    try {
        const {status} = await instance.put(`/teams/teamDelete`,{
            teamNo : route.params.teamNo,
            userNo : userNo.value
        })
        if(status ===200)    {
            alert('팀 탈퇴 완료')
            router.push({name:'teamList'})
        }
    } catch (error) {
        console.error("탈퇴실패",error)
    }
}
</script>

<style lang="scss" scoped>

</style>