<template>
  <div>
    <h3>채팅방 생성</h3>
    <input v-model="roomName" placeholder="채팅방 이름 입력" />

    <h4>팀원 선택:</h4>
    <div v-if="users.length">
      <div v-for="user in filteredUsers" :key="user.userNo">
        <label>
          <input
            type="checkbox"
            :value="user.userNo"
            v-model="selectedUserNos"
          />
          {{ user.name }}
        </label>
      </div>
    </div>
    <button @click="createRoom">생성</button>
  </div>
</template>

<script setup>
import { ref, computed , onMounted } from 'vue'
import { useUserStore } from '@/store/userStore'
import { useRouter , useRoute } from 'vue-router'
import instance from '@/util/interceptors'

const userStore = useUserStore()

const router = useRouter()
const route = useRoute()
const roomName = ref('')
const users = ref([])
const selectedUserNos = ref([])
const teamNo = Number(route.params.teamNo)

const currentUserNo = userStore.user?.userNo

// ✅ 현재 로그인한 사용자를 제외한 필터링된 목록
const filteredUsers = computed(() => {
  return users.value.filter(user => user.userNo !== currentUserNo)
})

// 팀원 목록 불러오기
const fetchTeamMembers = async () => {
  if (!teamNo) {
    console.error("❌ teamNo가 없습니다.");
    return;
  }

  try {
    const res = await instance.get(`/teams/${teamNo}/members`)
    users.value = res.data
    console.log("✅ 팀원 목록:", users.value)
  } catch (err) {
    console.error("❌ 팀원 목록 불러오기 실패:", err)
  }
}

// 채팅방 생성
const createRoom = async () => {
  if (!roomName.value.trim()) {
    alert('방 이름을 입력하세요')
    return
  }
  
  try {
    const response = await instance.post('/chat/room', {
      roomName: roomName.value,
      teamNo: teamNo,  // 필요한 경우 팀 번호 같이 전달
      memberUserNos: selectedUserNos.value, // 선택한 팀원 번호 배열
      creatorUserNo: userStore.user.userNo   // ✅ 현재 로그인한 유저 번호
    })

    if(response.status === 200) {
      alert('초대 및 방 생성 성공!');
      // 이후 처리
    } else {
      alert('초대 실패');
    }
    
    console.log('✅ 채팅방 생성 성공:', response.data)
    
    // ✅ 성공 후 생성된 방으로 이동
    router.push(`/room/${response.data.roomNo}`)
  } catch (error) {
    console.error('❌ 채팅방 생성 실패:', error)
    alert('채팅방 생성에 실패했습니다.')
  }
}
onMounted(() => {
  fetchTeamMembers()
})
</script>