<template>
  <div v-if="showModal" class="modal-backdrop" @click.stop>
 <div class="modal" @click.stop>
  <h3 class="modal-title">새 일정 등록</h3>

  <div class="modal-body">
    <div class="left-panel">
      <label>제목</label>
      <input v-model="formData.title" type="text" :disabled="formData.calDetailNo && !isEditMode" />
      <label>내용</label>
      <textarea v-model="formData.contents" :disabled="formData.calDetailNo && !isEditMode"></textarea>
    </div>

    <div class="right-panel">
      <label>Team Member</label>
      <div class="team-section">
        <div class="member-list">
          <div
            class="member-item"
            v-for="member in teamMembers"
            :key="member.userNo"
          >
            <input
              type="checkbox"
              :id="'participant-' + member.userNo"
              :value="member.userNo"
              v-model="formData.participantUserNos"
              :disabled="(!isEditMode && formData.calDetailNo) || formData.regUserNo === member.userNo || member.isSelf"
            />
            <label :for="'participant-' + member.userNo">
              {{ member.name }}
              <span v-if="formData.regUserNo === member.userNo"> (등록자)</span>
            </label>
          </div>
        </div>
      </div>

    <!-- 시작/종료 날짜 & 시간 영역 묶기 -->
    <div class="datetime-group">
      <div class="datetime-line">
        <label>시작 날짜:</label>
        <input v-model="formData.startDate" type="date" :disabled="formData.calDetailNo && !isEditMode" />
      </div>

      <div class="datetime-line">
        <label>시작 시간:</label>
        <input v-model="formData.startTime" type="time" :disabled="formData.calDetailNo && !isEditMode" />
      </div>

      <div class="datetime-line">
        <label>종료 날짜:</label>
        <input v-model="formData.endDate" type="date" :disabled="formData.calDetailNo && !isEditMode" />
      </div>

      <div class="datetime-line">
        <label>종료 시간:</label>
        <input v-model="formData.endTime" type="time" :disabled="formData.calDetailNo && !isEditMode" />
      </div>
    </div>
    </div>
  </div>

  <!-- 버튼 영역 -->
  <div class="modal-buttons">
    <button v-if="!formData.calDetailNo" @click="$emit('save')">등 록</button>
    <button v-if="formData.calDetailNo && !isEditMode && formData.regUserNo === userStore.user?.userNo" @click="isEditMode = true">수 정</button>
    <button v-if="isEditMode" @click="$emit('save')">저 장</button>
    <button @click="$emit('close')">닫 기</button>
    <button v-if="formData.calDetailNo && formData.regUserNo === userStore.user?.userNo" @click="$emit('delete')">❌ 삭 제</button>
  </div>
</div>
</div>

</template>

<script>
import { useUserStore } from '@/store/userStore';
import { ref, watch } from 'vue'

export default {
  props: {
    showModal: Boolean,
    formData: Object,
    teamMembers: Array,
  },
  setup(props) {
    const userStore = useUserStore();
    const isEditMode = ref(false);

    watch(() => props.showModal, (newVal) => {
      if (newVal) {
        isEditMode.value = false;
      }
    });

    return { userStore , isEditMode };
  },
};
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  max-width: 850px;
  height: 750px;
  padding: 24px 20px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
  background-color: #fafafa;
  border: 1px solid #ddd;
  overflow-y: auto;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translate(-50%, -60%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}

.modal-title {
  text-align: center;
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 50px;
  color: #333;
}

.modal textarea
{
  width: 100%;
  font-size: 1rem;
  padding: 8px 10px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background-color: #fff;
  box-sizing: border-box;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
  resize: none;
  height: 350px;
}

.modal input
{
  width: 100%;
  font-size: 1rem;
  padding: 8px 10px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background-color: #fff;
  box-sizing: border-box;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
  resize: none;
}

.modal input[type="text"]:hover,
.modal textarea:hover,
.modal input[type="text"]:focus,
.modal textarea:focus,
.time-input:hover,
.time-input:focus {
  border-color: #007bff;
  outline: none;
  box-shadow: 0 0 8px rgba(0, 123, 255, 0.5);
}

/* 버튼 컨테이너 */
.modal-buttons {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 40px;
}

/* 버튼 스타일 */
.modal-buttons button {
  padding: 10px 16px;
  font-size: 16px;
  border: none;
  border-radius: 6px;
  background-color: #007bff;
  color: white;
  cursor: pointer;
  transition: background-color 0.3s ease, box-shadow 0.3s ease;
}

.modal-buttons button:hover {
  background-color: #0056b3;
  box-shadow: 0 0 8px rgba(0, 123, 255, 0.6);
}

/* 체크박스 */
.modal input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #007bff;
  cursor: pointer;
  transition: transform 0.2s ease;
  margin: 0;
  flex-shrink: 0;
}

.modal input[type="checkbox"]:hover,
.modal input[type="checkbox"]:focus {
  outline: none;
  box-shadow: 0 0 6px rgba(0, 123, 255, 0.5);
}

.member-list {
  width: 290px;
  height: 200px;
  overflow-y: auto;
  margin-bottom: 12px;
  border: 1px solid #ccc;
  padding: 8px;
  border-radius: 6px;
  background-color: #fefefe;
  transition: box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.member-list:hover {
  box-shadow: 0 0 6px rgba(0, 123, 255, 0.2);
}

.member-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background-color 0.3s ease;
  width: 100%;
}

.member-item:hover {
  background-color: #f0f8ff;
}

.member-item input[type="checkbox"] {
  accent-color: #007bff;
  transition: transform 0.2s ease;
}
.member-item input[type="checkbox"]:checked {
  transform: scale(1.2);
}

.member-list::-webkit-scrollbar {
  width: 6px;
}

.member-list::-webkit-scrollbar-thumb {
  background-color: #ffffff;
  border-radius: 6px;
}

.member-list::-webkit-scrollbar-track {
  background-color: transparent;
}
.team-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modal-body {
  display: flex;
  gap: 20px;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.left-panel {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
}

.right-panel {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
}

.right-panel .team-section {
  width: 100%;
}

.right-panel .member-list {
  width: 100%;
}

.left-panel > label,
.right-panel > label:not(.team-section label) {
  display: block;
  text-align: center;
  margin-bottom: 12px;
  font-weight: 600;
  color: #333;
}

.team-section label {
  text-align: left;
  margin-bottom: 0;
}

.datetime-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.datetime-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.datetime-line label {
  width: 100px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

.datetime-line input[type="date"],
.datetime-line input[type="time"] {
  flex: 1;
  padding: 6px 10px;
  font-size: 0.95rem;
  border-radius: 6px;
  border: 1px solid #ccc;
}

</style>
