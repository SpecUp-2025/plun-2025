<template>
    <div>
    <div v-if="files.length > 0" class="file-list">
        <div
            v-for="(file, index) in files"
            :key="index"
            class="file-item"
          >
            <span>📄 {{ file.name }}</span>
            <button @click="removeFile(index)">❌</button>
          </div>
        </div>
    <div class="chat-input">
    
    <textarea
      v-model="inputMessage"
      class="message-textarea"
      @input="onInputChange"
      @keyup.enter="onKeydownEnter"
      @keydown.down.prevent="moveAutocomplete(1)"
      @keydown.up.prevent="moveAutocomplete(-1)"
      @keydown.enter.prevent="selectAutocomplete"
      placeholder="메시지를 입력하세요"
      rows="2"
    ></textarea>

    <!-- 아이콘 액션 그룹 -->
    <div class="input-actions">
      <button @click="send">⤴️</button>
      <button @click="triggerFileInput">📁</button>
    </div>
    <ul v-if="showAutocomplete" class="autocomplete-list">
      <li
        v-for="(member, index) in autocompleteList"
        :key="member.userNo"
        :class="{ selected: index === autocompleteIndex }"
        @click="selectMention(member)"
      >
        @{{ member.userName }}
      </li>
    </ul>
    <!-- 파일 input은 숨김 -->
    <input
      type="file"
      ref="fileInput"
      @change="onFileChange"
      multiple
      style="display: none"
    />
    <!-- 파일 선택 버튼 + 파일 개수 -->
    <div class="file-select-group">
      <button @click="triggerFileInput">파일 선택</button>
      <span v-if="files.length > 0">파일 {{ files.length }}개 선택됨</span>
    </div>
  </div>
  </div>
</template>

<script>
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore';

export default {
    name: 'ChatInput',
    emits: ['send-message'],

      props: { 
        chatMembers: { type: Array, required: true }, 
        roomNo: { type: Number, required: true },
        teamNo: { type: Number, required: true },
      },

    data() {
        return {
        inputMessage: '',
        files: [],
        mentions: [],
        showAutocomplete: false,
        autocompleteList: [],
        autocompleteIndex: -1
        }
    },
    methods: {

      onKeydownEnter(event) {
        if (this.showAutocomplete && this.autocompleteList.length > 0) {
          this.selectAutocomplete();
        } else {
          this.send();
        }
      },

      onInputChange() {
        const cursorIndex = this.inputMessage.lastIndexOf('@');
        if (cursorIndex !== -1) {
          const keyword = this.inputMessage.slice(cursorIndex + 1).trim();

          // 입력된 문자열이 존재할 경우만 자동완성 시작
          if (keyword.length > 0) {
            this.autocompleteList = this.chatMembers.filter(member =>
              member.userName.toLowerCase().includes(keyword.toLowerCase())
            );
            this.showAutocomplete = true;
            this.autocompleteIndex = 0;
          } else {
            this.showAutocomplete = false;
            this.autocompleteIndex = -1;
          }
        } else {
          this.showAutocomplete = false;
          this.autocompleteIndex = -1;
        }
      },

      moveAutocomplete(direction) {
        if (!this.showAutocomplete) return;
        const max = this.autocompleteList.length - 1;
        this.autocompleteIndex =
          (this.autocompleteIndex + direction + this.autocompleteList.length) %
          this.autocompleteList.length;
      },

      selectAutocomplete() {
        const member = this.autocompleteList[this.autocompleteIndex];
        if (member) {
          this.selectMention(member);
        }
        this.showAutocomplete = false;
      },

      selectMention(member) {
        const cursorIndex = this.inputMessage.lastIndexOf('@');
        if (cursorIndex !== -1) {
          // 입력된 '@' 이후 문자열 제거하고, 멘션 삽입
          this.inputMessage =
            this.inputMessage.slice(0, cursorIndex) + '@' + member.userName + ' ';
        }

        // 멘션 객체로 저장 (userNo와 userName 모두 포함)
        const mentionExists = this.mentions.some(m => m.userNo === member.userNo);
        if (!mentionExists) {
          this.mentions.push({
            userNo: member.userNo,
            userName: member.userName
          });
        }

        this.showAutocomplete = false;
        this.autocompleteList = [];
        this.autocompleteIndex = -1;
      },

        triggerFileInput() {
          this.$refs.fileInput.click();
        },
        onFileChange(event) {
          const newFiles = Array.from(event.target.files);
          this.files = this.files.concat(newFiles);
          this.$refs.fileInput.value = '';
        },
        removeFile(index) {
          this.files.splice(index, 1);
        },

        async send() {
          console.log('📌 전송 직전 roomNo:', this.roomNo);
        if (!this.inputMessage.trim() && this.files.length === 0) return;

            const userStore = useUserStore();
            const userNo = userStore.user?.userNo;

        if (!userNo) {
          console.warn('❌ 로그인된 사용자 정보가 없습니다.');
          return;
        }

        // mentions에서 userNo만 추출
        const mentionUserNos = this.mentions.map(m => m.userNo);

        // 파일이 하나라도 있으면 multipart 전송
        if (this.files.length > 0) {
          const formData = new FormData();

          const messageDTO = {
            roomNo: this.roomNo,
            userNo: userNo,
            content: this.inputMessage,
            messageType: 'FILE',
            mentions: mentionUserNos
          };

        formData.append('message', new Blob([JSON.stringify(messageDTO)], { type: 'application/json' }));
        this.files.forEach(file => {
          formData.append('file', file);
        });
        try {
          const response = await instance.post('/chat/send', formData
          );
          console.log('📁 파일 전송 성공:', response.data);
          this.inputMessage = '';
          this.files = [];
          this.mentions = [];
          this.$refs.fileInput.value = '';

          
          this.$emit('message-sent', response.data);

        } catch (error) {
          console.error('❌ 파일 전송 실패:', error);
        }

      } else {
        this.$emit('send-message', {
          content: this.inputMessage,
          mentions: mentionUserNos
        });
        this.inputMessage = '';
        this.mentions = [];
      }
    }
  }
}
</script>

<style scoped>

.message-textarea {
  flex: 1;
  max-width: 600px;
  padding: 12px 16px;
  font-size: 1rem;
  border: 1px solid #ccc;
  border-radius: 12px;
  resize: none;
}

.chat-input {
  position: fixed;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid #ddd;
  background-color: #fafafa;
  position: relative;
  gap: 12px;
}

.chat-input > input {
  flex: 1;
  max-width: 600px;
  padding: 50px 30px;
  font-size: 1rem;
  border: 5px solid #ccc;
  border-radius: 24px;
  outline: none;
  line-height: 1.4;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-actions button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  color: #555;
}

.input-actions button:hover {
  color: #000;
}

.file-select-group {
  display: none;
}

/* 자동완성 리스트 */
.autocomplete-list {
  position: absolute;
  bottom: 100%; /* top: 100%에서 bottom: 100%로 변경 - 입력창 위쪽에 표시 */
  left: 0; /* left: 42%에서 left: 0으로 변경 */
  transform: none; /* transform 제거 */
  width: 100%; /* 입력창과 같은 너비 */
  max-width: 600px; /* textarea와 동일한 max-width */
  border: 1px solid #ccc;
  background: white;
  list-style: none;
  padding: 0;
  margin: 0 0 4px 0; /* margin-top을 margin-bottom으로 변경 */
  max-height: 150px;
  overflow-y: auto;
  z-index: 10;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
}
.autocomplete-list li {
  padding: 6px 10px;
  cursor: pointer;
}
.autocomplete-list li.selected {
  background-color: #f0f0f0;
}
.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  margin-top: 8px;
  font-size: 0.9rem;
}
.file-item {
  display: inline-flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 4px;
  max-width: 300px;
  box-sizing: border-box;
}

.file-item span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
  margin-right: 12px;
}

.file-item button {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #999;
}
.file-item button:hover {
  color: #333;
}
</style>
