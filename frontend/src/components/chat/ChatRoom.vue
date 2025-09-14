<template>
  <div class="chat-room">

    <div class="room-name">
    <template v-if="isEditingRoomName">
        <input v-model="newRoomName" />
        <button @click="saveRoomName">저장</button>
        <button @click="cancelEditRoomName">취소</button>
    </template>
    <template v-else>
        <h2>
        {{ roomName }}
        <button @click="startEditRoomName">✏️</button>
        </h2>
    </template>
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
    :current-user-no="userNo"
    :chatMembers="chatMembers"
    @check-empty-message="removeMessageIfEmpty"
    @attachment-deleted="handleAttachmentDeleted"
    />
    </div>
    <ChatInput 
    :chatMembers="chatMembers" 
    @send-message="handleSendMessage"
    />
    </div>
    <!-- "화면만 나가기" -->
    <button @click="temporaryLeaveChatRoom">뒤로가기</button>
    <!-- "정말 나가기" -->
    <button @click="realLeaveChatRoom">채팅방 나가기</button>

</template>

<script>
import ChatMessage from './ChatMessage.vue';
import ChatInput from './ChatInput.vue';
import instance from '@/util/interceptors'
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { useUserStore } from '@/store/userStore';

export default {
  name: 'ChatRoom',
  components: { ChatMessage, ChatInput },
  data() {
    return {
        userStore: useUserStore(), // ✅ 이렇게 선언
        messages: [],
        stompClient: null,   // 추가
        stompConnected: false,
        chatMembers: [],
        roomName: '',            // ✅ 현재 채팅방 이름
        isEditingRoomName: false,
        newRoomName: '' ,         // ✅ 수정 입력값
        alarms: []
        }
  },
  mounted() {
    if (!this.userNo) {
    console.warn('❌ 로그인되지 않았습니다.');
    this.$router.push('/login');
    return;
  }
  console.log("✅ 현재 로그인한 사용자:", this.userStore.user);
    this.roomNo = Number(this.$route.params.roomNo); 
    this.registerChatMember(); // 참여자 등록
    this.loadMessages(); // 메시지 데이터
    this.connectWebSocket(); // 웹소켓
    this.loadChatMembers(); // 참여자목록
    this.loadRoomInfo(); // ✅ 채팅방 이름 불러오기
  },
    computed: {
    filteredMessages() {
      return this.messages.filter(
        msg => msg && (msg.content || (msg.attachments && msg.attachments.length > 0))
      );
    },  
    userNo() {
    return this.userStore.user?.userNo;
    }
  },

  methods: {

    async loadRoomInfo() {
        try {
            const res = await instance.get(`/chat/room/${this.roomNo}`);
            this.roomName = res.data.roomName;
        } catch (err) {
            console.error("❌ 채팅방 정보 불러오기 실패:", err);
        }
        },

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
    // 채팅방 퇴장
    // 잠깐 나가기
    temporaryLeaveChatRoom() {
        console.log("📤 채팅방에서 화면만 나감 (참여자 유지)");
        this.$router.push('/chat');
    },

    // 진짜 나가기 — 참여자 제거 + WebSocket 구독 해제
    async realLeaveChatRoom() {
        try {
            await instance.delete(`/chat/room/${this.roomNo}/member/${this.userNo}`);
            if (this.stompClient && this.stompConnected) {
                this.stompClient.disconnect(() => {
                    console.log("🔌 WebSocket 연결 종료됨");
                });
            }
            console.log("🚪 채팅방 영구 나가기 성공");
            this.$router.push('/chat');
        } catch (error) {
            console.error("❌ 채팅방 나가기 실패:", error);
        }
    },

    async leaveChatRoom() {
        const confirm = window.confirm("정말로 이 채팅방을 완전히 나가시겠습니까?");
        if (confirm) {
            await this.realLeaveChatRoom();
        } else {
            this.temporaryLeaveChatRoom();
        }
    },
    // 참여자 등록
    async registerChatMember() {
            try {
                await instance.post(`/chat/room/${this.roomNo}/member/${this.userNo}`);
                console.log("✅ 참여자 등록 성공");
            } catch (error) {
                console.error("❌ 참여자 등록 실패:", error);
            }
        },
    // 참여자 목록
    async loadChatMembers() {
        try {
            const response = await instance.get(`/chat/rooms/${this.roomNo}/members`);
            this.chatMembers = response.data;
            console.log("👥 참여자 목록 불러오기 성공:", this.chatMembers);
        } catch (error) {
            console.error("❌ 참여자 목록 불러오기 실패:", error);
        }
        },
    // 기존 메시지
    async loadMessages() {
        try {
            const response = await instance.get(`/chat/room/${this.roomNo}/messages`);

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

    handleSendMessage(payload) {
        if (!this.stompConnected) {
        console.warn('⚠️ WebSocket 연결이 되어 있지 않습니다.');
        return;
        }
        const message = {
            roomNo: this.roomNo,
            userNo: this.userNo, // 로그인 되면 변경예정
            content: payload.content,
            createDate: new Date().toISOString(),
            mentions: payload.mentions || []
        };

        console.log('➡️ 전송 준비 메시지:', message);
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
        },
        startEditRoomName() {
            this.isEditingRoomName = true;
            this.newRoomName = this.roomName;
        },
        cancelEditRoomName() {
            this.isEditingRoomName = false;
            this.newRoomName = '';
        },
        async saveRoomName() {
        try {
        await instance.put(`/chat/room/${this.roomNo}/name`, {
            roomName: this.newRoomName
        });
        this.roomName = this.newRoomName;
        this.isEditingRoomName = false;
        alert('채팅방 이름이 변경되었습니다.');
        } catch (err) {
        console.error("❌ 채팅방 이름 변경 실패:", err);
        alert('이름 변경에 실패했습니다.');
        }
    },

    // WebSocket 연결
    connectWebSocket() {
      const socket = new SockJS('/ws-chat'); // 백엔드 설정과 일치
      this.stompClient = Stomp.over(socket); // 기존 Client(...) 대신

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
        
        // timestamp 없으면 현재 시각으로 보정
        if (!received.timestamp && received.createDate) {
            received.timestamp = new Date(received.createDate).getTime();
        }
            this.messages.push(received);
            console.log('📝 messages 배열 업데이트:', this.messages);
        });
        // 참여자 목록 구독
        this.stompClient.subscribe(`/topic/chat/room/${this.roomNo}/members`, (msg) => {
            const members = JSON.parse(msg.body);
            console.log('👥 실시간 참여자 목록 수신:', members);
            this.chatMembers = members;
        });
        // 알림 구독 추가
        this.stompClient.subscribe(`/topic/notifications/${this.userNo}`, async (msg) => {
        const alarm = JSON.parse(msg.body);
        console.log('🔔 알림 수신 전체:', alarm);
        if (
            alarm.alarmType === 'CHAT' &&
            Number(alarm.referenceNo) === Number(this.roomNo) // 현재 방 알림이면
        ) {
            // 읽음 처리 API 호출 (예: alarmNo를 사용)
            try {
            await instance.put(`/alarms/${alarm.alarmNo}/read`);
            console.log(`✅ 알림 ${alarm.alarmNo} 읽음 처리 완료`);
            } catch (e) {
            console.error(`❌ 알림 ${alarm.alarmNo} 읽음 처리 실패`, e);
            }
        } else if (
            alarm.alarmType === 'CHAT' &&
            Number(alarm.referenceNo) !== Number(this.roomNo)
        ) {
            // 다른 방 알림이면 알림 배열에 추가 및 토스트 알림
            this.alarms.push(alarm);
        } else {
            console.log('채팅방에 있어 알림 무시:', alarm);
        }
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
