<template>
  <div class="chat-message">
    <strong>{{ message.sender || 'User ' + message.userNo }}</strong>
          <small>{{ formattedTime }}</small>

    <!-- 첨부파일 존재 여부 -->
    <div v-if="message.attachments && message.attachments.length > 0">
        <div v-for="file in message.attachments" :key="file.attachmentNo">
            <!-- 이미지 파일이면 미리보기 -->
            <template v-if="isImage(file.contentType)">
                <img
                :src="`/api/attachments/download/${file.attachmentNo}`"
                alt="image preview"
                style="max-width: 200px; max-height: 150px;"
                />
            </template>

            <!-- 이미지가 아니면 링크 -->
            <template v-else>
                <a :href="`/api/attachments/download/${file.attachmentNo}`" target="_blank" rel="noopener noreferrer">
                {{ file.originalName }}
                </a>
            </template>
            <!-- 삭제 버튼 -->
            <button @click="deleteAttachment(file.attachmentNo)">삭제</button>
        </div>
    </div>

    <!-- 일반 메시지 -->
    <div v-if="message.content">
      {{ message.content }}
    </div>
  </div>
</template>

<script>
import axios from 'axios'; 

export default {
  name: 'ChatMessage',
  props: {
    message: {
      type: Object,
      required: true
    }
  },
  computed: {
    formattedTime() {
      const date = new Date(this.message.timestamp);
      return date.toLocaleTimeString();
    }
  },
    methods: {
        isImage(contentType) {
        return contentType.startsWith('image/');
        },

    async deleteAttachment(attachmentNo) {
      if (!confirm('정말 이 파일을 삭제하시겠습니까?')) return;

      try {
       await axios.delete(`/api/attachments/delete/${attachmentNo}`, {
        withCredentials: true
        }); // ✅ 백엔드 API로 삭제 요청

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
  margin-bottom: 8px;
}
small {
  color: gray;
  margin-left: 10px;
  font-size: 0.75em;
}
</style>
