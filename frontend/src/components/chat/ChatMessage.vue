<template>
  <div class="chat-message" :class="isMine ? 'my-message' : 'other-message'">

    <div class="message-row">
    <!-- 내 메시지면 시간 먼저 -->
      <small v-if="isMine" class="chat-time">{{ formattedTime }}</small>
    <div class="message-bubble" :class="{ 'mention-message': hasMentions }">
      <div class="chat-info">
        <strong v-if="!isMine">{{ senderName }}</strong>
      </div>

      <!-- 멘션 메시지 -->
      <div v-if="message.content" v-html="highlightMentions(message.content)"></div>
      <!-- 첨부파일 -->
      <div v-if="message.attachments && message.attachments.length > 0">
        <div
          v-for="file in message.attachments"
          :key="file.attachmentNo"
          class="attachment"
        >
          <!-- 이미지 -->
          <template v-if="isImage(file.contentType)">
            <img
              class="preview-image"
              :src="`/api/attachments/download/${file.attachmentNo}`"
              alt="image preview"
            />
          </template>

          <!-- 파일 링크 -->
          <template v-else>
            <a
              :href="`/api/attachments/download/${file.attachmentNo}`"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ file.originalName }}
            </a>
          </template>

          <!-- 삭제 버튼 -->
          <button v-if="currentUserNo === message.userNo" @click="deleteAttachment(file.attachmentNo)">삭제</button>
        </div>
      </div>
    </div>
        <small v-if="!isMine" class="chat-time">{{ formattedTime }}</small>
  </div>
  </div>
</template>


<script>
import instance from '@/util/interceptors'

export default {
  name: 'ChatMessage',
  props: {
    message: { type: Object, required: true },
    currentUserNo: { type: Number, required: true },
    chatMembers: { type: Array, required: true}
  },
    computed: {
      isMine() {
          return this.message.userNo === this.currentUserNo
        },
      formattedTime() {
          const date = new Date(this.message.timestamp);
          return date.toLocaleTimeString();
        },
      senderName() {
        return this.message.name || '탈퇴한 사용자';
        },
      hasMentions() {
          // message.mentions 배열이 있다고 가정
          return this.message.mentions && this.message.mentions.length > 0;
        }
    },
    methods: {

        isImage(contentType) {
        return contentType.startsWith('image/');
        },

        highlightMentions(text) {
        // 멘션 패턴 예: 또는 @이름
        // 멘션 부분만 <span class="mention"> 태그로 감싸기
        if (!this.hasMentions) return this.escapeHtml(text);

        // 멘션 목록을 이용해 텍스트 내 멘션을 하이라이트 처리
        let highlighted = this.escapeHtml(text);

        this.message.mentions.forEach(mention => {
          // mention: { userId: '관리자1', userName: '관리자1' } 형태 가정
          const mentionText = `@${mention.userName}`;
          const mentionRegex = new RegExp(`(${mentionText})`, 'g');
          highlighted = highlighted.replace(
            mentionRegex,
            `<span class="mention">$1</span>`
          );
        });

        return highlighted;
      },

      escapeHtml(text) {
        // XSS 방지용 간단한 escape
        return text.replace(/[&<>"']/g, function(match) {
          const escape = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
          };
          return escape[match];
        });
      },

    async deleteAttachment(attachmentNo) {
      if (!confirm('정말 이 파일을 삭제하시겠습니까?')) return;

      try {
       await instance.delete(`/attachments/delete/${attachmentNo}`, {
        withCredentials: true
        }); //  백엔드 API로 삭제 요청

        // 삭제 성공 시 프론트에서 해당 파일 제거
        this.message.attachments = this.message.attachments.filter(
        file => file.attachmentNo !== attachmentNo
        );

        // 삭제 후 비어있는 메시지인지 부모에 알림
        this.$emit('check-empty-message', this.message);

            // 부모에게 삭제된 첨부파일 정보 emit (WebSocket 브로드캐스트용)
        this.$emit('attachment-deleted', {
        messageNo: this.message.messageNo,
        attachmentNo: attachmentNo
        });

        alert('삭제되었습니다.');
      } catch (error) {
        console.error('❌ 파일 삭제 실패:', error);
        alert('파일 삭제에 실패했습니다.');
      }
    }
  }
};
</script>

<style scoped>
.chat-message {
  display: flex;
  flex-direction: column;
  margin: 15px 0;
}
/* 내 메시지 → 오른쪽 정렬 */
.my-message {
  align-items: flex-end;
}
/* 상대 메시지 → 왼쪽 정렬 */
.other-message {
  align-self: flex-start;
}
.chat-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.8em;
  color: gray;
  margin-bottom: 6px;
}
.chat-time {
  font-size: 0.8rem;
  color: gray;
  margin-top: 0;
  margin-bottom: 4px;
  margin-right: 8px;
  margin-left: 8px;
  align-self: flex-end;
  line-height: 1;
}
.message-row {
  display: flex;
  align-items: flex-end;
}
.my-message .chat-info {
  flex-direction: row-reverse;
}
/* 말풍선 */
.message-bubble {
  background-color: #f1f1f1;
  padding: 10px 20px;
  border-radius: 18px;
  min-width: auto;
  max-width: 400px;
  display: inline-block;
  word-wrap: break-word;
  box-sizing: border-box;
  line-height: 1.4;
  font-size: 1rem;
  width: fit-content;
  white-space: pre-wrap;
}
.my-message .message-bubble {
  background-color: #d0eaff;
}
.mention-message {
  background-color: #fff7e6;
  border-left: 3px solid #ffa500;
  padding-left: 5px;
}
.mention {
  font-weight: bold;
  color: #d46b08;
  background-color: #fff1b8;
  border-radius: 3px;
  padding: 0 3px;
}
/* 첨부파일 */
.preview-image {
  max-width: 200px;
  max-height: 150px;
  margin-top: 6px;
}
.attachment {
  margin-bottom: 4px;
}
</style>
