<template>
    <form @submit.prevent="login">
      <input v-model="form.email" type="email" name="username"
             autocomplete="username" placeholder="이메일을 입력해주세요" />
      <br />
      <input v-model="form.password" type="password" name="password"
             autocomplete="current-password" placeholder="비밀번호를 입력해주세요" />
      <button type="submit" :disabled="pending">{{ pending ? '로그인 중...' : '로그인' }}</button>
    </form>
    <RouterLink :to="{name : 'register'}" >회원가입</RouterLink>
  </template>
  
  <script setup>
import { getUser } from '@/util/getUser'
import instance from '@/util/interceptors'
import { reactive, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

const router = useRouter()
const pending = ref(false)
const form = reactive({ email: '', password: '' })

const login = async () => {
  if (!form.email || !form.password) return alert('이메일/비밀번호를 입력하세요.')
  pending.value = true
  try {
    const { data } = await instance.post(`/auth/login`, {
      email: form.email.trim(),
      password: form.password,
    })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    await getUser()
    alert('로그인 성공입니다.')
    await router.push({ name: 's' })
  } catch (error) {
    console.error('로그인 실패', error)
    alert(error.response?.data?.message ?? '로그인 실패')
  } finally {
    pending.value = false
  }
}
</script>
  