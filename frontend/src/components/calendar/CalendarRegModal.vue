<template>
  <div v-if="showModal" class="modal-backdrop" @click="$emit('close')">
    <div class="modal" @click.stop>
      <h3 class="modal-title">새 일정 등록</h3>

      <!-- 팀원 초대 -->
      <label>Team Member</label>
      <div style="max-height: 150px; overflow-y: auto; margin-bottom: 12px; border: 1px solid #ccc; padding: 8px; border-radius: 6px;">
        <div
          v-for="member in teamMembers"
          :key="member.userNo"
          style="margin-bottom: 6px;"
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

      <label>제목</label>
      <input v-model="formData.title" type="text" :disabled="formData.calDetailNo && !isEditMode" />

      <label>내용</label>
      <textarea v-model="formData.contents" :disabled="formData.calDetailNo && !isEditMode"></textarea>

      <label>시작 시간</label>
      <input v-model="formData.startTime" type="time" class="time-input" :disabled="formData.calDetailNo && !isEditMode"/>

      <label>종료 시간</label>
      <input v-model="formData.endTime" type="time" class="time-input" :disabled="formData.calDetailNo && !isEditMode"/>

      <br />
      <div class="modal-buttons">
        <button v-if="!formData.calDetailNo" @click="$emit('save')">등록하기</button>
        <button v-if="formData.calDetailNo && !isEditMode && formData.regUserNo === userStore.user?.userNo" @click="isEditMode = true">✏️ 수정하기</button>
        <button v-if="isEditMode" @click="$emit('save')">저장하기</button>
        <button @click="$emit('close')">닫기</button>
        <button v-if="formData.calDetailNo && formData.regUserNo === userStore.user?.userNo" @click="$emit('delete')">❌일정 삭제</button>
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

    // 모달 열릴 때 수정 모드 초기화
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
  width: 90%;
  max-width: 400px;
  padding: 24px 20px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;

  background-color: #fafafa;
  border: 1px solid #ddd;
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
  margin-bottom: 20px;
  color: #333;
}

/* 인풋, 텍스트에어리어, 시간 입력 */
.modal input[type="text"],
.modal textarea,
.time-input {
  width: 100%;
  font-size: 1rem;
  padding: 8px 10px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background-color: #fff;
  box-sizing: border-box;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
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
  margin-top: 20px;
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
  accent-color: #007bff;
  cursor: pointer;
  transition: box-shadow 0.3s ease;
}

.modal input[type="checkbox"]:hover,
.modal input[type="checkbox"]:focus {
  outline: none;
  box-shadow: 0 0 6px rgba(0, 123, 255, 0.5);
}

</style>
