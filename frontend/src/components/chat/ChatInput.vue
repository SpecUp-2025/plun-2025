<template>
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

    <!-- 선택된 파일 목록 표시 -->
    <div v-if="files.length > 0" class="file-list">
      <div
        v-for="(file, index) in files"
        :key="index"
        class="file-item"
      >
        📄 {{ file.name }}
        <button @click="removeFile(index)">❌</button>
      </div>
    </div>
  </div>
</template>

<script>
import instance from '@/util/interceptors'
import { useUserStore } from '@/store/userStore';

export default {
    name: 'ChatInput',

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

        // 멘션 중복 방지
        if (!this.mentions.includes(member.userNo)) {
          this.mentions.push(member.userNo);
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
          console.log('📌 전송 직전 roomNo:', this.roomNo);  // 👈 이거 추가
        if (!this.inputMessage.trim() && this.files.length === 0) return;

            const userStore = useUserStore();
            const userNo = userStore.user?.userNo;

        if (!userNo) {
          console.warn('❌ 로그인된 사용자 정보가 없습니다.');
          return;
        }

      // ✅ 파일이 하나라도 있으면 multipart 전송
      if (this.files.length > 0) {
        const formData = new FormData();

        const messageDTO = {
          roomNo: this.roomNo,
          //roomNo: this.$route.params.roomNo,
          userNo: userNo,
          content: this.inputMessage,
          messageType: 'FILE',
          mentions: this.mentions
        };

        formData.append('message', new Blob([JSON.stringify(messageDTO)], { type: 'application/json' }));
        this.files.forEach(file => {
          formData.append('file', file); // 파일 여러 개 추가
        });
        try {
          const response = await instance.post('/chat/send', formData
          );
          console.log('📁 파일 전송 성공:', response.data);
          this.inputMessage = '';
          this.files = [];
          this.mentions = [];
          this.$refs.fileInput.value = '';

          // 옵션: 백엔드에서 WebSocket 브로드캐스트 한다면 생략 가능
          this.$emit('message-sent', response.data);

        } catch (error) {
          console.error('❌ 파일 전송 실패:', error);
        }

      } else {
        // 일반 메시지는 WebSocket으로 전송
        this.$emit('send-message', {
          content: this.inputMessage,
          mentions: this.mentions
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
  top: 100%;
  left: 42%;
  transform: translateX(-50%);
  width: calc(100% - 20px);
  max-width: 400px;
  border: 1px solid #ccc;
  background: white;
  list-style: none;
  padding: 0;
  margin: 4px 0 0 0;
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
  margin-top: 8px;
  font-size: 0.9rem;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 4px;
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
