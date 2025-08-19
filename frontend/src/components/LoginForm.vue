<template>
    <div>
        <input type="text" placeholder="이메일을 입력해주세요" v-model="form.userid"/><br/>
        <input type="text"placeholder="비밀번호를 입력해주세요"v-model="form.passwd"/>
        <button @click.prevent="login"></button>
    </div>
</template>

<script setup>
import axios from 'axios';
import { reactive } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const form = reactive({
    userid :'',
    passwd : '',
});

const login = async () => {
    try {
        const {status,data} = await axios.post('/api/auth/login',{
            email : form.userid,
            password : form.passwd,
        })
        if(status ==200){
            localStorage.setItem('accessToken', data.accessToken)
            localStorage.setItem('refreshToken', data.refreshToken)

      // 2) 이후 요청에 Authorization 자동 첨부
      const authHeader = `${data.tokenType ?? 'Bearer'} ${data.accessToken}`
      axios.defaults.headers.common.Authorization = authHeader

            alert("로그인 성공입니다.")
            router.push({name : 's'});
        }
    } catch (error) {
        console.error("로그인 실패 ",error);
    }
}

</script>

<style lang="scss" scoped>

</style>