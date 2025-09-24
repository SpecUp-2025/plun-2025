<template>
  <div class="chat-room">
      <div class="room-name">
          <template v-if="isEditingRoomName">
              <input v-model="newRoomName" />
                <button class="save-btn" @click="saveRoomName">이름 변경</button>
                <button class="cancel-btn" @click="cancelEditRoomName">그대로</button>
            </template>
            <template v-else>
<div class="room-header-wrapper">
  <h2 class="room-header">
    <span class="room-name-text" @click="startEditRoomName">{{ roomName }}</span>
  </h2>
  <button @click="leaveChatRoom" class="btn btn-exit">❌</button>
</div>
    </template>
</div>
<div class="chat-date">{{ todayDate }}</div>
    
    <!-- 참여자 목록 표시 -->
    <div class="chat-body">
    <div class="chat-content">
        <!-- 채팅 메시지 -->
        <section class="chat-messages">
            <ChatMessage
            v-for="msg in filteredMessages"
            :key="msg.messageNo + '-' + (msg.attachments ? msg.attachments.length : 0)"
            :message="msg"
            :current-user-no="userNo"
            :chatMembers="chatMembers"
            @check-empty-message="removeMessageIfEmpty"
            @attachment-deleted="handleAttachmentDeleted"
            />
        </section>
        
        <!-- 참여자 목록 표시 -->
        <aside class="chat-members">
            <p><strong>💬 참여자 목록 ({{ chatMembers.length }})</strong></p>
            <div v-for="member in chatMembers" :key="member.userNo" class="chat-member-item">
            <img v-if="member.profileImage" :src="member.profileImage" alt="프로필" class="member-avatar" />
            <div class="member-info">
                <div class="member-name">{{ member.userName }}</div>
                <div class="member-team">{{ member.teamName }}</div>
            </div>
            </div>
        </aside>
        </div>
        <ChatInput
        class="chat-input"
        :room-no="roomNo"
        :team-no="teamNo"
        :chatMembers="chatMembers" 
        @send-message="handleSendMessage"
        />
        </div>
        </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import ChatMessage from './ChatMessage.vue';
import ChatInput from './ChatInput.vue';
import instance from '@/util/interceptors'
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { useUserStore } from '@/store/userStore';
import { useAlarmStore } from '@/store/useAlarmStore'

const props = defineProps({
  roomNo: { type: Number, required: true },
  teamNo: { type: Number, required: true }
});

const emit = defineEmits(['closeRoom', 'alarmRead', 'roomNameChanged']);

const userStore = useUserStore();
const alarmStore = useAlarmStore();
const router = useRouter();

const messages = ref([])
const roomName = ref('')
const stompClient = ref(null)
const stompConnected = ref(false)
const chatMembers = ref([]);
const isEditingRoomName = ref(false);
const newRoomName = ref('');

const userNo = computed(() => userStore.user?.userNo)

const filteredMessages = computed(() =>
  messages.value.filter(
    msg => msg && (msg.content || (msg.attachments && msg.attachments.length > 0))
  )
)


const todayDate = computed(() => {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth() + 1;
  const day = today.getDate();
  return `${year}년 ${month}월 ${day}일`;
});

const loadRoomInfo = async () => {
  try {
    const res = await instance.get(`/chat/room/${props.roomNo}`);
    roomName.value = res.data.roomName;
  } catch (err) {
    console.error("❌ 채팅방 정보 불러오기 실패:", err);
  }
};

const handleAttachmentDeleted = ({ messageNo, attachmentNo }) => {
  if (!stompConnected.value || !stompClient.value) return;
  const payload = {
    type: 'DELETE_ATTACHMENT',
    roomNo: props.roomNo,
    messageNo,
    attachmentNo
  };
  console.log('🗑️ 첨부파일 삭제 브로드캐스트:', payload);
  stompClient.value.send('/app/chat.deleteAttachment', {}, JSON.stringify(payload));
};

const removeMessageIfEmpty = (message) => {
  const isEmpty = !message.content && (!message.attachments || message.attachments.length === 0);
  if (isEmpty) {
    messages.value = messages.value.filter(m => m.messageNo !== message.messageNo);
    console.log(`🗑️ 메시지 ${message.messageNo} 삭제됨 (내용 없음)`);
  }
};

const realLeaveChatRoom = async () => {
  try {
    await instance.delete(`/chat/room/${props.roomNo}/member/${userNo.value}`);
    if (stompClient.value && stompConnected.value) {
      stompClient.value.disconnect(() => {
        console.log("🔌 WebSocket 연결 종료됨");
      });
    }
    console.log("🚪 채팅방 영구 나가기 성공");
    emit('closeRoom');
  } catch (error) {
    console.error("❌ 채팅방 나가기 실패:", error);
  }
};

const leaveChatRoom = async () => {
  const confirmLeave = window.confirm(
    "정말로 이 채팅방을 완전히 나가시겠습니까?\n\n(채팅 종료를 하시면 더이상 이 채팅방의 알림을 가지않습니다.)"
  );
  if (confirmLeave) {
    await realLeaveChatRoom();
  }
};

const registerChatMember = async () => {
  try {
    await instance.post(`/chat/room/${props.roomNo}/member/${userNo.value}`);
    console.log("✅ 참여자 등록 성공");
  } catch (error) {
    console.error("❌ 참여자 등록 실패:", error);
  }
};

const loadChatMembers = async () => {
  try {
    const response = await instance.get(`/chat/rooms/${props.roomNo}/members`);
    chatMembers.value = response.data;
    console.log("👥 참여자 목록 불러오기 성공:", chatMembers.value);
  } catch (error) {
    console.error("❌ 참여자 목록 불러오기 실패:", error);
  }
};

const loadMessages = async () => {
  try {
    const response = await instance.get(`/chat/room/${props.roomNo}/messages`);

    messages.value = response.data.map(msg => ({
      ...msg,
      timestamp: new Date(msg.createDate).getTime()
    }));
    console.log('✅ 초기 메시지 불러오기 성공:', messages.value);

    await nextTick();
    const messageContainer = document.querySelector('.chat-messages');
    if (messageContainer) {
      messageContainer.scrollTop = messageContainer.scrollHeight;
    }
  } catch (e) {
    console.error('메시지 불러오기 실패', e);
  }
};

const handleSendMessage = (payload) => {
  if (!stompConnected.value) {
    console.warn('⚠️ WebSocket 연결이 되어 있지 않습니다.');
    return;
  }
  const message = {
    roomNo: props.roomNo,
    userNo: userNo.value,
    content: payload.content,
    createDate: new Date().toISOString(),
    mentions: payload.mentions || []
  };

  console.log('➡️ 전송 준비 메시지:', message);
  if (stompClient.value && stompClient.value.connected) {
    console.log('➡️ 메시지 발신:', message);
    stompClient.value.send(
      '/app/chat.sendMessage',
      {},
      JSON.stringify(message)
    );
  } else {
    console.warn('⚠️ stompClient가 없거나 연결되어 있지 않습니다.');
  }
};

const startEditRoomName = () => {
  isEditingRoomName.value = true;
  newRoomName.value = roomName.value;
};

const cancelEditRoomName = () => {
  isEditingRoomName.value = false;
  newRoomName.value = '';
};

const saveRoomName = async () => {
  try {
    await instance.put(`/chat/room/${props.roomNo}/name`, {
      roomName: newRoomName.value
    });
    roomName.value = newRoomName.value;
    isEditingRoomName.value = false;
    alert('채팅방 이름이 변경되었습니다.');
  } catch (err) {
    console.error("❌ 채팅방 이름 변경 실패:", err);
    alert('이름 변경에 실패했습니다.');
  }
};

const connectWebSocket = () => {
  const socket = new SockJS('/ws-chat');
  stompClient.value = Stomp.over(socket);

  stompClient.value.connect({}, () => {
    console.log('✅ WebSocket 연결 성공');
    stompConnected.value = true;

    stompClient.value.subscribe(`/topic/chat/room/${props.roomNo}`, (msg) => {
      console.log('⬅️ 서버로부터 메시지 수신:', msg.body);
      const received = JSON.parse(msg.body);

      if (received.type === 'DELETE_ATTACHMENT') {
        const msgToUpdate = messages.value.find(m => m.messageNo === received.messageNo);
        if (msgToUpdate) {
          msgToUpdate.attachments = msgToUpdate.attachments.filter(
            file => file.attachmentNo !== received.attachmentNo
          );
          const isEmpty = !msgToUpdate.content && msgToUpdate.attachments.length === 0;
          if (isEmpty) {
            messages.value = messages.value.filter(m => m.messageNo !== msgToUpdate.messageNo);
          }
        }
        return;
      }

      // 채팅방 이름 변경 처리
      if (received.type === 'ROOM_NAME_UPDATE') {
        roomName.value = received.roomName;
        
        emit('roomNameChanged', {
          roomNo: received.roomNo,
          roomName: received.roomName
        });
        return;
      }

      if (!received.timestamp && received.createDate) {
        received.timestamp = new Date(received.createDate).getTime();
      }
      messages.value.push(received);

      nextTick(() => {
        const messageContainer = document.querySelector('.chat-messages');
        if (messageContainer) {
          messageContainer.scrollTop = messageContainer.scrollHeight;
        }
      });
    });

    stompClient.value.subscribe(`/topic/chat/room/${props.roomNo}/members`, (msg) => {
      const members = JSON.parse(msg.body);
      chatMembers.value = members;
    });

    stompClient.value.subscribe(`/topic/notifications/${userNo.value}`, async (msg) => {
      const alarm = JSON.parse(msg.body);
      console.log('🔔 알림 수신 전체:', alarm);
      if (
        alarm.alarmType === 'CHAT' &&
        Number(alarm.referenceNo) === Number(props.roomNo) && isChatActive.value
      ) {
        try {
          await instance.put(`/alarms/${alarm.alarmNo}/read`);
          console.log(`✅ 알림 ${alarm.alarmNo} 읽음 처리 완료`);
          emit('alarmRead', alarm.alarmNo);
        } catch (e) {
          console.error(`❌ 알림 ${alarm.alarmNo} 읽음 처리 실패`, e);
        }
      } else if (
        alarm.alarmType === 'CHAT' &&
        Number(alarm.referenceNo) !== Number(props.roomNo)
      ) {
        alarmStore.addAlarm(alarm);
      } else {
        console.log('채팅방에 있어 알림 무시:', alarm);
      }
    });
  }, (error) => {
    console.error('❌ WebSocket 연결 실패:', error);
  });
};

const isChatActive = ref(false);
onMounted(() => {
  if (!userNo.value) {
    console.warn('❌ 로그인되지 않았습니다.')
    return
  }
  console.log("✅ 현재 로그인한 사용자:", userStore.user)
  registerChatMember()
  loadMessages()
  connectWebSocket()
  loadChatMembers()
  loadRoomInfo()

  nextTick(() => {
    const messageContainer = document.querySelector('.chat-messages')
    if (messageContainer) {
      const activateChat = () => {
        if (!isChatActive.value) {
          isChatActive.value = true
          console.log('💡 채팅 활성 상태로 변경됨');
        }
      };
      messageContainer.addEventListener('scroll', activateChat, { once: true })
      messageContainer.addEventListener('mouseenter', activateChat, { once: true })
    }
  })
});
</script>

<style scoped>
input {
  height: 40px;
  width: 300px;
  font-size: 16px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  box-sizing: border-box;
}

.save-btn,
.cancel-btn {
  background-color: #153E75;
  color: white;
  border: none;
  padding: 10px 20px;
  font-size: 16px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 10px;
  transition: background-color 0.3s ease;
}

.save-btn:hover {
  background-color: #0056b3;
}

.cancel-btn {
  background-color: #6c757d;
}

.cancel-btn:hover {
  background-color: #5a6268;
}

.room-name-text {
  cursor: pointer;
  user-select: none;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.chat-date {
  font-size: 1rem;
  color: #444;
  margin-top: 6px;
  font-weight: 500;
  text-align: center;
  border: none;
  border-radius: 6px;
  display: inline-block;
}

.room-header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.room-header {
  margin: 0;
  flex: 1;
  text-align: center;
  font-weight: bold;
  font-size: 1.25rem;
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-room {
  height: 85vh;
  display: flex;
  flex-direction: column;
}

.room-name {
  position: sticky;
  top: 0;
  background: white;
  padding: 12px 16px;
  border-bottom: 1px solid #ddd;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-content {
  flex: 1;
  display: flex;
  min-height: 0;
}

.chat-members {
  width: 200px;
  background-color: #fafafa;
  border-right: 1px solid #ddd;
  padding: 10px;
  overflow-y: auto;
}

.chat-messages {
  flex: 1;
  padding: 12px;
  background: white;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  min-height: 0;
}

.chat-input {
  position: sticky;
  bottom: 0;
  background: white;
  padding: 8px 12px;
  border-top: 1px solid #ddd;
  z-index: 10;
}

.messages {
    border: 1px solid #ddd;
    padding: 10px;
}

.chat-members {
  margin-top: 10px;
  padding: 10px;
  background-color: #fafafa;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.chat-member-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.member-info {
  display: flex;
  flex-direction: column;
}

.member-role {
  font-size: 12px;
  color: #888;
}

.member-name {
  font-weight: bold;
  font-size: 14px;
}

.member-team {
  font-size: 12px;
  color: #666;
}

.btn-exit {
  color: white;
  border: none;
  border-radius: 10px;
  padding: 6px 12px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.btn-exit:hover {
  background-color: #153E75;
}
</style>