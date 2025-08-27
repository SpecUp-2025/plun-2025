<script setup>
const year = new Date().getFullYear()
</script>
<template>
  <footer class="ftr"><slot>© {{ year }}</slot></footer>
</template>
<style scoped>
.ftr{background:#ffd8e6;border-top:1px solid #ffc2db;color:#6b2f45;padding:8px 14px;font-size:12px;}
</style>
