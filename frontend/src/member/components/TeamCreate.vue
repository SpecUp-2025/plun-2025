<template>
    <div>
        <h2>팀만들기</h2>
        <input v-model="form.teamName" type="text" placeholder="팀 제목을 입력해주세요"></input>
        <input v-model.trim="inputEmail" type="email" 
            placeholder="example@example.com" 
            @keydown.enter.prevent="add"
            @keydown.space.prevent="add"
            @keyup="onComma"></input>
        <div v-show ="unEmailVaild" v-text="unEmailVaild"></div>
        <button type="button" @click="add">추가</button>

        <ul>
            <li v-for="(e,i) in form.invite" :key="e">
            - {{ e }}
                <button @click="remove(i)">삭제</button>
            </li>
        </ul>
        <button @click="create">초대하기</button>
        <button @click="router.push({name:'teamList'})">취소</button>
    </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { REGEX_PATTERN } from '../util/Regex';
import instance from '@/util/interceptors';
import { useUserStore } from '@/store/userStore';

const router = useRouter()
const userStore = useUserStore();
const userNo = computed(() => userStore.user?.userNo ?? '')
const userEmail = computed(() => userStore.user?.email ?? '')
const userName = computed(() => userStore.user?.name ?? '')
const form = reactive({
    teamName:'',
    invite:[],
})
const inputEmail = ref('')
const unEmailVaild = ref('')

const create = async () => {
    if (Object.values(unEmailVaild.value).some(v => !!v)) return;
    if(!form.teamName.trim()){
        alert("팀 이름을 입력해주세요")
        return;
    }
    const email  = (userEmail.value ?? '').trim().toLowerCase()
    try {
        const {status,data} = await instance.post(`teams/createTeam`,{
            userNo : userNo.value,
            invite: form.invite,
            teamName : form.teamName,
            email,
            userName:userName.value

        })
        if(status ===201){
            alert("팀 생성했습니다.")
            console.log(data.teamName)
            router.push({name:'teamMain',params:{teamNo:data.teamNo}})
        }
    } catch (error) {
        console.error('팀을 생성하지 못했습니다.',error);
        alert("팀을 생성하지 못했습니다.")
    }
    
   
}

function addEmail(){
    const email = inputEmail.value.trim().toLowerCase();
    const me  = (userEmail.value ?? '').trim().toLowerCase()
    if(!email) return;
    if(!REGEX_PATTERN.EMAIL.test(email)){
        unEmailVaild.value = '올바르지 않은 이메일 형식입니다.'
        return false;
    }
    if(email === me){
        unEmailVaild.value = '본인의 이메일은 추가 못합니다.'
        return false;
    }
    unEmailVaild.value='';
    if(!form.invite.includes(email)) form.invite.push(email)
    return true;
}

function add(){
    if (!inputEmail.value) return
    const ok = addEmail()  
    if (ok) inputEmail.value = ''
}

function remove(i){
    form.invite.splice(i,1)
}
function onComma (e) {
  if (e.key === ',') {
    addEmail()
    e.preventDefault()
  }
}
</script>

<style lang="scss" scoped>

</style>