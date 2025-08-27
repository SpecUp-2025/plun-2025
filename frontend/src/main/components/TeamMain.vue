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
        <router-link :to="{ name: 'TeamList' }">← 팀 목록으로</router-link>
      </div>
      <MeetingNav :team-no="teamNo" />
    </template>

    <div>여기에 팀 메인 컨텐츠</div>

    <template #footer>© PLUN</template>
  </DefaultLayout>
</template>
