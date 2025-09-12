<template>
    <form @submit.prevent="login">
      <input v-model="form.email" type="email" name="email"
             autocomplete="email" placeholder="이메일을 입력해주세요" @input="checkEmail" />
      
      <div v-show ="unvaild.email" v-text="unvaild.email"></div> 
      <br />
      <input v-model="form.password" type="password" name="password"
             autocomplete="current-password" placeholder="비밀번호를 입력해주세요" />
      <button type="submit" >로그인</button>
    </form>
    <button @click="socialLogin('google')">구글로 로그인</button>
    <button @click="socialLogin('naver')">네이버로 로그인</button>
    <button @click="socialLogin('kakao')">카카오톡으로 로그인</button>
    <RouterLink :to="{name : 'register'}" >회원가입</RouterLink>
    <RouterLink :to="{name : 'find'}" >비밀번호 찾기</RouterLink>
  </template>
  
  <script setup>
import { REGEX_PATTERN } from '@/member/util/Regex'
import { getUser } from '@/util/getUser'
import axios from 'axios'
import { reactive, } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const form = reactive({ email: '', password: '' })

const unvaild = reactive({
    email:'',
})

const login = async () => {
  checkEmail();
  if (Object.values(unvaild).some(v => !!v)) return;
  const email = form.email?.trim().toLowerCase();
  if (!email || !form.password) return alert('이메일/비밀번호를 입력하세요.')
  try {
    const { data,status } = await axios.post(`api/auth/login`, {
      email,
      password: form.password,
    })
    if(status ===200){
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await getUser()
      alert('로그인 성공입니다.')
      await router.push({ name: 'teamList' })
    }
    
  } catch (error) {
    console.error('로그인 실패', error)
    alert('이메일/비밀번호를 확인해주세요')
  }
}


function checkEmail() {
  if (!REGEX_PATTERN.EMAIL.test(form.email)) {
    unvaild.email = "올바르지 않은 이메일 형식입니다.";
    return;
  }
  unvaild.email = "";
}

function socialLogin(name) {
  window.location.href = `/api/oauth/${name}/login`

}
</script>
