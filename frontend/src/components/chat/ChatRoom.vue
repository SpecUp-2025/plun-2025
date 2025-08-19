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
    <div class="messages">
      <ChatMessage v-for="msg in messages" :key="msg.id" :message="msg" />
    </div>
    <ChatInput @send-message="handleSendMessage" />
  </div>
  <button @click="leaveChatRoom">나가기</button>
</template>

<script>
import ChatMessage from './ChatMessage.vue';
import ChatInput from './ChatInput.vue';
import axios from 'axios';
import SockJS from 'sockjs-client';
//import { Client } from '@stomp/stompjs';
import Stomp from 'stompjs'; // stompjs v2.x

export default {
  name: 'ChatRoom',
  components: { ChatMessage, ChatInput },
  data() {
    return {
      messages: [],
      stompClient: null,   // 추가
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
  methods: {
    
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
        const response = await axios.get(`/api/chat/room/${this.roomNo}/Messages`);
        this.messages = response.data;
        console.log('✅ 초기 메시지 불러오기 성공:', this.messages);
      } catch (e) {
        console.error('메시지 불러오기 실패', e);
      }
    },

    handleSendMessage(newMsg) {
      const message = {
        roomNo: this.roomNo,
        userNo: this.userNo, // 로그인 되면 변경예정
        //userNo: 1,this.currentUserNo, 실제 로그인된 유저 번호를 넣어야 함
        content: newMsg,
        timestamp: Date.now()
      };
      //this.messages.push(message);

    // WebSocket으로 서버에 전송
      if (this.stompClient && this.stompClient.connected) {
        console.log('➡️ 메시지 발신:', message);
        this.stompClient.send(
          '/app/chat.sendMessage', // 백엔드에서 처리하는 경로
          {},
          JSON.stringify(message)
        );
      } else {
      console.warn('⚠️ stompClient가 없거나 연결되어 있지 않습니다.');
    }

      // 화면에 즉시 반영
      // this.messages.push(message);
    },

    // WebSocket 연결
    connectWebSocket() {
      const socket = new SockJS('/ws-chat'); // ✅ 백엔드 설정과 일치
      this.stompClient = Stomp.over(socket); // ✅ 기존 Client(...) 대신

      this.stompClient.connect({}, () => {
        console.log('✅ WebSocket 연결 성공');

        this.stompClient.subscribe(`/topic/chat/room/${this.roomNo}`, (msg) => {
          console.log('⬅️ 서버로부터 메시지 수신:', msg.body);
          const received = JSON.parse(msg.body);
        // ✅ timestamp 없으면 현재 시각으로 보정
        if (!received.timestamp) {
            received.timestamp = Date.now();
        }
          this.messages.push(received);
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
