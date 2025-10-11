<template>
  <div class="chat-message" :class="isMine ? 'my-message' : 'other-message'">
    <div class="message-row">
      <!-- 내 메시지면 시간 먼저 -->
      <small v-if="isMine" class="chat-time">{{ formattedTime }}</small>

      <!-- 텍스트가 있을 때 말풍선 출력 -->
      <div v-if="message.content" class="message-bubble" :class="{ 'mention-message': hasMentions }">
        <div class="chat-info">
          <strong v-if="!isMine">{{ senderName }}</strong>
        </div>

        <div v-html="highlightMentions(message.content)"></div>
      </div>

      <!-- 첨부파일만 있을 때, 또는 텍스트가 있든 없든 첨부파일은 항상 말풍선 밖에 출력 -->
      <div v-if="message.attachments && message.attachments.length > 0" class="attachments-outside-bubble">
        <div
          v-for="file in message.attachments"
          :key="file.attachmentNo"
          class="attachment"
          @contextmenu="handleFileRightClick(file, $event)"
        >
          <template v-if="isImage(file.contentType)">
            <img
              class="preview-image"
              :src="`/api/attachments/download/${file.attachmentNo}`"
              alt="image preview"
              @click="preventDefault"
            />
          </template>
          <template v-else>
            <a
              :href="`/api/attachments/download/${file.attachmentNo}`"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ file.originalName }}
            </a>
          </template>
        </div>
      </div>

      <small v-if="!isMine" class="chat-time">{{ formattedTime }}</small>
    </div>
    <div
      v-if="contextMenuVisible"
      :style="{ top: `${contextMenuPosition.top}px`, left: `${contextMenuPosition.left}px` }"
      class="context-menu"
    >
      <ul>
        <li @click="saveAttachment">저장</li>
        <li v-if="isMine" @click="deleteAttachment(contextFile.attachmentNo)">삭제</li>
      </ul>
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
    data() {
    return {
      contextMenuVisible: false,
      contextMenuPosition: { top: 0, left: 0 },
      contextFile: null,
    };
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
          return this.message.mentions && this.message.mentions.length > 0;
        }
    },
    methods: {

        isImage(contentType) {
        return contentType.startsWith('image/');
        },

        handleFileRightClick(file, event) {
          event.preventDefault();
          this.contextFile = file;
          this.contextMenuPosition = { top: event.clientY, left: event.clientX };
          this.contextMenuVisible = true;
        },

        convertUrlsToLinks(text) {
          // URL 패턴 정규식 (http, https 지원)
          const urlPattern = /(https?:\/\/[^\s]+)/g;
          
          return text.replace(urlPattern, (url) => {
            return `<a href="${url}" target="_blank" rel="noopener noreferrer" class="message-link">${url}</a>`;
          });
        },

      highlightMentions(text) {
        // 1. HTML escape
        let processed = this.escapeHtml(text);

        // 2. 멘션 하이라이트
        if (this.hasMentions) {
          this.message.mentions.forEach(mention => {
            const mentionText = `@${mention.userName}`;
            const mentionRegex = new RegExp(`(${this.escapeRegex(mentionText)})`, 'g');
            processed = processed.replace(
              mentionRegex,
              `<span class="mention">$1</span>`
            );
          });
        }

        // 3. URL을 링크로 변환
        processed = this.convertUrlsToLinks(processed);

        return processed;
      },

      convertUrlsToLinks(text) {
        // URL 패턴: http:// 또는 https://로 시작
        const urlPattern = /(https?:\/\/[^\s<]+)/g;
        
        return text.replace(urlPattern, (url) => {
          return `<a href="${url}" target="_blank" rel="noopener noreferrer" class="message-link">${url}</a>`;
        });
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

        escapeRegex(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      },

    async deleteAttachment(attachmentNo) {

      if (!this.contextFile) return;

      if (!confirm('정말 이 파일을 삭제하시겠습니까?')) return;

      try {
       await instance.delete(`/attachments/delete/${attachmentNo}`, {
        withCredentials: true
        });

        this.message.attachments = this.message.attachments.filter(
        file => file.attachmentNo !== attachmentNo
        );

        this.$emit('check-empty-message', this.message);

        // 부모에게 삭제된 첨부파일 정보 emit (WebSocket 브로드캐스트용)
        this.$emit('attachment-deleted', {
        messageNo: this.message.messageNo,
        attachmentNo: attachmentNo
        });

        alert('삭제되었습니다.');
        this.contextMenuVisible = false;
      } catch (error) {
        console.error('❌ 파일 삭제 실패:', error);
        alert('파일 삭제에 실패했습니다.');
      }
      this.contextMenuVisible = false;
    },

    preventDefault(event) {
      event.preventDefault();
    },

    saveAttachment() {
      const fileUrl = `/api/attachments/download/${this.contextFile.attachmentNo}`;
      const a = document.createElement('a');
      a.href = fileUrl;
      a.download = this.contextFile.originalName;
      a.click();
      this.contextMenuVisible = false;
    },

    closeContextMenu(event) {
      if (!this.$el.contains(event.target)) {
        this.contextMenuVisible = false;
      }
    }
  },
  watch: {
    contextMenuVisible(newVal) {
      if (newVal) {
        document.addEventListener('mousedown', this.closeContextMenu);
      } else {
        document.removeEventListener('mousedown', this.closeContextMenu);
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
/* 내 메시지 */
.my-message {
  align-items: flex-end;
}
/* 상대 메시지 */
.other-message {
  align-items: flex-start;
}
.message-row {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
}
.my-message .message-row {
  justify-content: flex-end;
}
.my-message .chat-time {
  order: 0;
  margin-left: 8px;
  margin-right: 8px;
  white-space: nowrap;
  font-size: 0.8rem;
  color: gray;
}
/* 말풍선 순서 */
.my-message .message-bubble {
  order: 1;
  background-color: #d0eaff;
}
.other-message .message-row {
  justify-content: flex-start;
}
.other-message .message-bubble {
  order: 0;
  background-color: #f1f1f1;
}
.other-message .chat-time {
  order: 1;
  margin-right: 8px;
  margin-left: 8px;
  white-space: nowrap;
  font-size: 0.8rem;
  color: gray;
}
.message-bubble {
  padding: 10px 20px;
  border-radius: 18px;
  max-width: 400px;
  display: inline-block;
  word-wrap: break-word;
  box-sizing: border-box;
  line-height: 1.4;
  font-size: 1rem;
  width: fit-content;
  white-space: pre-wrap;
}

.mention-message {
  border-left: 10px solid #1890ff;
  padding-left: 12px;
  background-color: rgba(24, 144, 255, 0.03);
}

.mention {
  font-weight: 600;
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.15);
  border-radius: 2px;
  padding: 1px 3px;
}
.preview-image {
  max-width: 200px;
  max-height: 150px;
  border-radius: 4px;
  cursor: pointer;
  display: block;
  margin: 10px 0;
}
.attachments-outside-bubble {
  display: flex;
  flex-direction: column;
  margin-top: 8px;
  max-width: 400px;
}
.context-menu {
  position: absolute;
  background-color: #fff;
  border: 1px solid #ddd;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  padding: 10px;
  border-radius: 4px;
  z-index: 1000;
}
.context-menu ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.context-menu li {
  padding: 8px;
  cursor: pointer;
}
.context-menu li:hover {
  background-color: #f0f0f0;
}
</style>
