<template>
  <div class="chat-room">
    <!-- 사용자 번호 입력창 테스트용-->
    <div>
      <label>사용자 번호: </label>
      <input type="number" v-model.number="userNo" min="1" />
    </div>

    <!-- 참여자 목록 표시 -->
        <div class="chat-members">
        참여자:
        <span v-for="member in chatMembers" :key="member.userNo" class="chat-member">
            {{ member.userName }}
        </span>
        </div>
    <!-- 채팅 메시지 -->
    <div><ChatMessage
      v-for="msg in filteredMessages"
    :key="msg.messageNo + '-' + (msg.attachments ? msg.attachments.length : 0)"
    :message="msg"
    @check-empty-message="removeMessageIfEmpty"
    @attachment-deleted="handleAttachmentDeleted"
    />
    </div>
    <ChatInput @send-message="handleSendMessage" />
  </div>
    <!-- 채팅방 생성 버튼 -->
    <button @click="goToCreateRoom">채팅방 생성</button>
    <button @click="leaveChatRoom">나가기</button>
</template>

<script>
import ChatMessage from './ChatMessage.vue';
import ChatInput from './ChatInput.vue';
import axios from 'axios';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs'; 

export default {
  name: 'ChatRoom',
  components: { ChatMessage, ChatInput },
  data() {
    return {
      messages: [],
      stompClient: null,   // 추가
      stompConnected: false,
      roomNo: null, // 임시로 하드코딩. 나중엔 route param에서 받아오는 게 이상적
      userNo: 1,  // 기본 유저 번호를 데이터에 추가 로그인 완료 후 변경예정
      chatMembers: []
    }
  },
  mounted() {
    this.roomNo = Number(this.$route.params.roomNo); 
    this.registerChatMember(); // 참여자 등록
    this.loadMessages(); // 메시지 데이터
    this.connectWebSocket(); // 웹소켓
    this.loadChatMembers(); // 참여자목록
  },
    computed: {
    filteredMessages() {
      return this.messages.filter(
        msg => msg && (msg.content || (msg.attachments && msg.attachments.length > 0))
      );
    }
  },

  methods: {

    handleAttachmentDeleted({ messageNo, attachmentNo }) {
        if (!this.stompConnected || !this.stompClient) return;

        const payload = {
            type: 'DELETE_ATTACHMENT',
            roomNo: this.roomNo,
            messageNo,
            attachmentNo
        };

        console.log('🗑️ 첨부파일 삭제 브로드캐스트:', payload);
        console.log('🗑️ 첨부파일 삭제 WebSocket 전송:', payload);
        this.stompClient.send('/app/chat.deleteAttachment', {}, JSON.stringify(payload));
        },

    removeMessageIfEmpty(message) {
        const isEmpty = !message.content && (!message.attachments || message.attachments.length === 0);

        if (isEmpty) {
            this.messages = this.messages.filter(m => m.messageNo !== message.messageNo);
            console.log(`🗑️ 메시지 ${message.messageNo} 삭제됨 (내용 없음)`);
        }
        },
    goToCreateRoom() {
      this.$router.push('/room/new');
      // 또는 이름 기반 라우팅이면:
      // this.$router.push({ name: 'ChatRoomForm' });
    },
    
    // 채팅방 퇴장
    async leaveChatRoom() {
        try {
            await axios.delete(`/api/chat/room/${this.roomNo}/member/${this.userNo}`);
            console.log("🚪 채팅방 나가기 성공");
            this.$router.push('/chat'); // 또는 이전 화면
        } catch (error) {
            console.error("❌ 채팅방 나가기 실패:", error);
        }
        },

    // 참여자 등록
    async registerChatMember() {
            try {
                await axios.post(`/api/chat/room/${this.roomNo}/member/${this.userNo}`);
                console.log("✅ 참여자 등록 성공");
            } catch (error) {
                console.error("❌ 참여자 등록 실패:", error);
            }
        },
    // 참여자 목록
    async loadChatMembers() {
        try {
            const response = await axios.get(`/api/chat/rooms/${this.roomNo}/members`);
            this.chatMembers = response.data;
            console.log("👥 참여자 목록 불러오기 성공:", this.chatMembers);
        } catch (error) {
            console.error("❌ 참여자 목록 불러오기 실패:", error);
        }
        },
    // 기존 메시지
    async loadMessages() {
        try {
        //const response = await axios.get(`/api/chat/message?roomNo=${this.roomNo}`);
        const response = await axios.get(`/api/chat/room/${this.roomNo}/messages`);

        // 서버 메시지 배열에서 createDate를 timestamp로 변환
        this.messages = response.data.map(msg => ({
        ...msg,
        timestamp: new Date(msg.createDate).getTime() // or Date.parse(msg.createDate)
        }));
        console.log('✅ 초기 메시지 불러오기 성공:', this.messages);
        } catch (e) {
        console.error('메시지 불러오기 실패', e);
        }
    },

    handleSendMessage(newMsg) {
    if (!this.stompConnected) {
    console.warn('⚠️ WebSocket 연결이 되어 있지 않습니다.');
    return;
    }
      const message = {
        roomNo: this.roomNo,
        userNo: this.userNo, // 로그인 되면 변경예정
        //userNo: 1,this.currentUserNo, 실제 로그인된 유저 번호를 넣어야 함
        content: newMsg,
        createDate: new Date().toISOString()
      };
      //this.messages.push(message);
      console.log('➡️ 전송 준비 메시지:', message);
    // WebSocket으로 서버에 전송
      if (this.stompClient && this.stompClient.connected) {
        console.log('➡️ 메시지 발신:', message);
        this.stompClient.send(
            '/app/chat.sendMessage', // 백엔드에서 처리하는 경로
            {}, // 추가
            JSON.stringify(message)
            );
      } else {
      console.warn('⚠️ stompClient가 없거나 연결되어 있지 않습니다.');
    }

      // 화면에 즉시 반영
      //this.messages.push(message);
      //this.messages.push({...message, timestamp: new Date(message.createDate).getTime()});
    },

    // WebSocket 연결
    connectWebSocket() {
      const socket = new SockJS('/ws-chat'); // ✅ 백엔드 설정과 일치
      this.stompClient = Stomp.over(socket); // ✅ 기존 Client(...) 대신

      this.stompClient.connect({}, () => {
        console.log('✅ WebSocket 연결 성공');
        this.stompConnected = true; 

        this.stompClient.subscribe(`/topic/chat/room/${this.roomNo}`, (msg) => {
          console.log('⬅️ 서버로부터 메시지 수신:', msg.body);
          const received = JSON.parse(msg.body);
        
          if (received.type === 'DELETE_ATTACHMENT') {
            const msgToUpdate = this.messages.find(m => m.messageNo === received.messageNo);
            if (msgToUpdate) {
            msgToUpdate.attachments = msgToUpdate.attachments.filter(
                file => file.attachmentNo !== received.attachmentNo
                );
                const isEmpty = !msgToUpdate.content && msgToUpdate.attachments.length === 0;
            if (isEmpty) {
                this.messages = this.messages.filter(m => m.messageNo !== msgToUpdate.messageNo);
                }
            }
            return;
        }
        
        // ✅ timestamp 없으면 현재 시각으로 보정
        if (!received.timestamp && received.createDate) {
          received.timestamp = new Date(received.createDate).getTime();
        }
          this.messages.push(received);
          console.log('📝 messages 배열 업데이트:', this.messages);
        });
      }, (error) => {
        console.error('❌ WebSocket 연결 실패:', error);
      });
    }
  }
};
</script>

<style scoped>
.messages {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ddd;
  padding: 10px;
}
</style>
