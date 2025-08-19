<template>
  <div class="chat-input">
    <input v-model="inputMessage" @keyup.enter="send" placeholder="메시지를 입력하세요" />
    <button @click="send">전송</button>
  </div>
</template>

<script>
export default {
  name: 'ChatInput',
  data() {
    return {
      inputMessage: ''
    }
  },
  methods: {
    send() {
      if(this.inputMessage.trim() === '') return;
      this.$emit('send-message', this.inputMessage);
      this.inputMessage = '';
    }
  }
}
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
