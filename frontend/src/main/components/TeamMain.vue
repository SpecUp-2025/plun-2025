<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import MeetingNav from '@/components/meeting/MeetingNav.vue'

const route = useRoute()
const teamNo = computed(() => Number(route.params.teamNo)) // :id 사용
</script>

<template>
  <DefaultLayout>
    <template #header><strong>Team Main</strong></template>

    <template #sidebar>
      <div style="margin-bottom:8px">
        <router-link :to="{ name: 'teamList' }">← 팀 목록으로</router-link>
      </div>
        <!-- 채팅방 생성 -->
        <router-link :to="{ name: 'ChatRoomForm', params: { teamNo } }">💬채팅방 만들기</router-link>
        <!-- 채팅방 목록-->
        <div> 
        <router-link :to="{ name: 'ChatRoomList' }">💬채팅방 목록</router-link>
        </div>
      <router-link :to="{ name: 'Calendar', params: { teamNo } }">📅 캘린더</router-link>
      <MeetingNav :team-no="teamNo" />
    </template>

    <div>여기에 팀 메인 컨텐츠</div>

    <template #footer>© PLUN</template>
  </DefaultLayout>
</template>
