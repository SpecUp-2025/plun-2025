<template>
    <div>
        로그인처리중
    </div>
</template>

<script setup>
import { getUser } from '@/util/getUser'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()

const params = new URLSearchParams(location.hash.slice(1))
const err = params.get('error')
const access = params.get('access')
const refresh = params.get('refresh')

history.replaceState(null, '', location.pathname + location.search)
onMounted(async () => {
  if (err) {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    alert('소셜 로그인에 실패했어요.')
    return router.replace({ name: 'login' })
  }

  if (access && refresh) {
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
    await getUser()
    return router.replace({ name: 'teamList' })
  }

  return router.replace({ name: 'login' })

})
</script>


<style lang="scss" scoped>

</style>