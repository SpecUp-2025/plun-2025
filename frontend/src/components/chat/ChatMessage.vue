<template>
  <div class="chat-message">
    <strong>{{ message.sender }}:</strong> {{ message.content }}
    <small>{{ formattedTime }}</small>
  </div>
</template>

<script>
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
  }
}
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
