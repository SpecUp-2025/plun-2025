<template>
    <div>
        <h2>성공입니당</h2>
        <button @click="check" >토큰 전송 확인</button>
        <button @clcik ="logout">로그아웃</button>
    </div>
</template>

<script setup>
import instance from '@/util/interceptors';

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
</script>

<style lang="scss" scoped>

</style>