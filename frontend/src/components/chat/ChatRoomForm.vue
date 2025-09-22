<template>
  <div class="container" role="main" aria-label="채팅방 생성">
    <h3 class="title">채팅방 생성</h3>

    <label for="roomName" class="input-label">채팅방 이름</label>
    <input
      id="roomName"
      v-model="roomName"
      type="text"
      placeholder="채팅방 이름을 입력하세요"
      class="input"
      autocomplete="off"
      aria-required="true"
    />

    <h4 class="subtitle">초대할 팀원 선택</h4>
    <div class="responsive-grid">
      <!-- 초대 전 팀원 -->
      <div class="user-column">
        <h5 class="user-column-title">초대 전</h5>
        <div
          v-for="user in filteredUsers.filter(u => !selectedUserNos.includes(u.userNo))"
          :key="'available-' + user.userNo"
          class="user-item"
        >
          <label class="checkbox-label">
            <input
              type="checkbox"
              :value="user.userNo"
              v-model="selectedUserNos"
              class="checkbox"
            />
            {{ user.name }}
          </label>
        </div>
      </div>

      <!-- 초대한 팀원 -->
      <div class="user-column">
        <h5 class="user-column-title">초대 완료</h5>
        <div
          v-for="user in filteredUsers.filter(u => selectedUserNos.includes(u.userNo))"
          :key="'invited-' + user.userNo"
          class="user-item invited"
        >
          <label class="checkbox-label">
            <input
              type="checkbox"
              :value="user.userNo"
              v-model="selectedUserNos"
              class="checkbox"
            />
            {{ user.name }}
          </label>
        </div>
      </div>
    </div>

    <button @click="createRoom" class="btn" type="button" aria-label="채팅방 생성">
      생성
    </button>
  </div>
</template>


<script setup>
/* 기존 스크립트 동일 */
import { ref, computed , onMounted } from 'vue'
import { useUserStore } from '@/store/userStore'
import { useRouter , useRoute } from 'vue-router'
import instance from '@/util/interceptors'

const emit = defineEmits(['roomCreated'])

const userStore = useUserStore()

const router = useRouter()
const route = useRoute()
const roomName = ref('')
const users = ref([])
const selectedUserNos = ref([])
const teamNo = Number(route.params.teamNo)

const currentUserNo = userStore.user?.userNo

const filteredUsers = computed(() => {
  return users.value.filter(user => user.userNo !== currentUserNo)
})

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

const createRoom = async () => {
  if (!roomName.value.trim()) {
    alert('방 이름을 입력하세요')
    return
  }
  
  try {
    const response = await instance.post('/chat/room', {
      roomName: roomName.value,
      teamNo: teamNo,
      memberUserNos: selectedUserNos.value,
      userNo: userStore.user.userNo
    })

    if(response.status === 200) {
      alert('초대 및 방 생성 성공!');
      emit('roomCreated', Number(response.data.roomNo))
    } else {
      alert('초대 실패');
    }
    
    console.log('✅ 채팅방 생성 성공:', response.data)
    
  } catch (error) {
    console.error('❌ 채팅방 생성 실패:', error)
    alert('채팅방 생성에 실패했습니다.')
  }
}

onMounted(() => {
  fetchTeamMembers()
})
</script>

<style scoped>
/* 전체 컨테이너 중앙 배치, 최대 너비 제한, 부드러운 그림자 추가 */
.container {
  max-width: 800px;
  margin: 50px auto;
  padding: 28px 32px;
  background-color: #f9faff;
  border-radius: 12px;
  border: 1px solid #3399ff;
  box-shadow: 0 8px 16px rgba(0, 123, 255, 0.15);
  text-align: center;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #1e2a42;
}

.title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #0069d9;
  margin-bottom: 24px;
  letter-spacing: 0.05em;
  user-select: none;
}

.input-label {
  display: block;
  text-align: left;
  margin-bottom: 6px;
  font-weight: 600;
  color: #339cff;
  font-size: 1rem;
}

.input {
  width: 100%;
  padding: 12px 14px;
  font-size: 1rem;
  border: 2px solid #1e90ff;
  border-radius: 6px;
  outline-offset: 2px;
  transition: border-color 0.25s ease;
  box-sizing: border-box;
  margin-bottom: 28px;
  user-select: text;
}

.input::placeholder {
  color: #a5c9ff;
}

.input:focus {
  border-color: #339cff;
  box-shadow: 0 0 6px #339cffaa;
}

.subtitle {
  font-size: 1.1rem;
  font-weight: 600;
  color: #339cff;
  margin-bottom: 14px;
  user-select: none;
  text-align: left;
}

.user-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f5ff;
  font-size: 1rem;
  color: #1e2a42;
  display: flex;
  align-items: center;
  user-select: none;
  transition: background-color 0.2s ease;
}

.user-item:last-child {
  border-bottom: none;
}

.user-item:hover {
  background-color: #e8f0ff;
}

.checkbox-label {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #339cff;
}

.btn {
  width: 100%;
  background-color: #0069d9;
  color: white;
  font-weight: 700;
  font-size: 1.15rem;
  padding: 14px 0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 5px 12px rgba(0, 105, 217, 0.35);
}

.btn:hover {
  background-color: #1e90ff;
  box-shadow: 0 6px 16px rgba(30, 144, 255, 0.6);
}

.btn:focus-visible {
  outline: 3px solid #339cff;
  outline-offset: 2px;
}

.user-list::-webkit-scrollbar {
  width: 8px;
}

.user-list::-webkit-scrollbar-thumb {
  background-color: #339cff88;
  border-radius: 4px;
}

.user-list::-webkit-scrollbar-track {
  background-color: #f9faff;
}
/* 좌우 열 배치 */
.responsive-grid {
  display: flex;
  flex-direction: row; /* 가로 배치 */
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.user-column {
  flex: 1 1 48%;
  background-color: #ffffff;
  border: 1px solid #dbe9ff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.03);
  min-height: 260px;
  overflow-y: auto;
}

.user-column-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #339cff;
  margin-bottom: 12px;
  border-bottom: 1px solid #eef5ff;
  padding-bottom: 6px;
}

/* 초대한 유저 강조 */
.user-item.invited {
  background-color: #e6f4ff;
}

/* 반응형 모바일 */
@media (max-width: 640px) {
  .responsive-grid {
    flex-direction: column;
  }

  .user-column {
    flex: 1 1 100%;
  }
}

</style>
