<template>
    <div>
        <h2>초대 현황</h2>

        <div v-if="!form.list.length">
            초대 받은 알림이 없습니다.
        </div>

        <ul v-else>
            <li v-for="(item,index) in form.list"  :key="item.invitedId">
                {{ index +1}}
                팀이름 : {{ item.teamName }},
                초대한 사람 : {{ item.invitedEmail }},
                초대한 날짜 : {{ item.createDate }},
                <button @click="accept(item.teamNo,item.invitedId)">수락하기</button>
                <button @click="cancel(item.invitedId)">거절하기</button>
            </li>
        </ul>



        <button @click="router.push({name:'teamList'})">취소</button>
        
    </div>
</template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter()
const userStore = useUserStore()
const userNo = computed(() => userStore.user?.userNo ?? '')
const form = reactive({
    list : [],
})

onMounted(async () => {
    await invitation()
})

const invitation = async () =>{
    try {
        const { status, data} = await instance.get(`/teams/invitation/${userNo.value}`)
        if(status ===200 ){
            form.list = data
        }
    } catch (error) {
        console.error("리스트를 불러오는데 실패했습니다.",error)
    }
    
}

const accept = async (teamNo,invitedId) =>{
    try {
        const {status} = await instance.post(`/teams/accept`,{
            teamNo,
            invitedId,
            userNo: userNo.value
        })
        if(status === 204){
            alert("수락 했습니다.")
            await invitation()
        }
    } catch (error) {
        console.error("수락 실패",error)
    }
}
const cancel = async (invitedId) =>{
    try {
        const {status} = await instance.put(`/teams/cancel/${invitedId}`)
        if(status === 204){
            alert("거절 했습니다.")
            await invitation()
        }
    } catch (error) {
        console.error("거절 실패",error)
    }
}
</script>

<style lang="scss" scoped>

</style>