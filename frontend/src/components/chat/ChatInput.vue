<template>
  <div class="chat-input">
    <input v-model="inputMessage" @keyup.enter="send" placeholder="메시지를 입력하세요" />
    <!-- 파일 선택 -->
    <input type="file" @change="onFileChange" />
    <button @click="send">전송</button>
  </div>
</template>

<script>

import axios from 'axios';

export default {
  name: 'ChatInput',
  data() {
    return {
      inputMessage: '',
      file: null
    }
  },
  methods: {
    onFileChange(event) {
      this.file = event.target.files[0];
    },

    async send() {
      if (!this.inputMessage.trim() && !this.file) return;

      // 파일이 있을 경우: HTTP 전송
      if (this.file) {
        const formData = new FormData();
        const messageDTO = {
          roomNo: this.$route.params.roomNo,
          userNo: 1,
          content: this.inputMessage,
          messageType: 'FILE'
        };

        formData.append('message', new Blob([JSON.stringify(messageDTO)], { type: 'application/json' }));
        formData.append('file', this.file);

        try {
          const response = await axios.post('/api/chat/send', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          });
          console.log('📁 파일 전송 성공:', response.data);
          this.inputMessage = '';
          this.file = null;

          // 옵션: 백엔드에서 WebSocket 브로드캐스트 한다면 생략 가능
          this.$emit('message-sent', response.data);

        } catch (error) {
          console.error('❌ 파일 전송 실패:', error);
        }

      } else {
        // 일반 메시지는 WebSocket으로 전송
        this.$emit('send-message', this.inputMessage);
        this.inputMessage = '';
      }
    }
  }
}

// import axios from 'axios';

// export default {
//   name: 'ChatInput',
//   data() {
//     return {
//       inputMessage: '',
//       file: null
//     }
//   },
//   methods: {
//     onFileChange(event) {
//         this.file = event.target.files[0];
//         },
//     async send() {
//         if (!this.inputMessage.trim() && !this.file) return;

//         const formData = new FormData();
//         const messageDTO = {
//             roomNo: this.$route.params.roomNo, // 또는 props로 받은 roomNo
//             userNo: 1, // 임시로 하드코딩하거나 로그인 유저로 교체
//             content: this.inputMessage,
//             messageType: this.file ? 'FILE' : 'TALK'
//         };

//         formData.append('message', new Blob([JSON.stringify(messageDTO)], { type: 'application/json' }));
//         if (this.file) {
//             formData.append('file', this.file);
//         }

//         try {
//             const response = await axios.post('/api/chat/send', formData, {
//             headers: {
//                 'Content-Type': 'multipart/form-data'
//             }
//             });

//             console.log('전송 성공:', response.data);
//             this.inputMessage = '';
//             this.file = null;
//             this.$emit('message-sent', response.data); // 필요시 상위로 알림
//         } catch (error) {
//             console.error('전송 실패:', error);
//         }
//         }
//     }
//     }

    // send() {
    //   if(this.inputMessage.trim() === '') return;
    //   this.$emit('send-message', this.inputMessage);
    //   this.inputMessage = '';
    // }
</script>

<style scoped>
.chat-input {
  margin-top: 10px;
}
input {
  width: 80%;
  padding: 8px;
}
button {
  padding: 8px 12px;
}
</style>
