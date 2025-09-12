<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import LocalMedia from "@/components/meeting/LocalMedia.vue";
import RemoteMedia from "@/components/meeting/RemoteMedia.vue";

const route = useRoute();
const router = useRouter();

const roomCode = computed(() => String(route.params.roomCode || ""));
const myPublisherId = ref("");

function onPublisherId(id) { myPublisherId.value = id || ""; }
function onLeave() { router.back(); }
</script>

<template>
  <div class="room">
    <div class="header">
      <div class="title">
        <strong>회의방</strong>
        <span class="code">#{{ roomCode }}</span>
      </div>
      <div class="right">
        <button class="btn danger" @click="onLeave">나가기</button>
      </div>
    </div>

    <div class="grid">
      <LocalMedia :room-code="roomCode" @publisher-id="onPublisherId" />
      <!-- key 로 재마운트 보장 -->
      <RemoteMedia
        :key="myPublisherId || 'no-pub'"
        :room-code="roomCode"
        :my-publisher-id="myPublisherId"
      />
    </div>
  </div>
</template>

<style scoped>
.room { padding: 12px; }
.header { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.title { display:flex; align-items:center; gap:8px; }
.title .code { color:#888; font-size:12px; }
.right .btn.danger { background:#7a1f1f; border-color:#a22; color:#fff; }
.grid { display:grid; grid-template-columns: 1fr 1fr; gap:12px; }
.btn { padding:6px 10px; border-radius:8px; background:#222; color:#fff; border:1px solid #444; cursor:pointer; }
.btn:hover { background:#333; }
</style>
