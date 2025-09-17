<script setup>
const year = new Date().getFullYear()
</script>

<template>
  <footer class="site-footer"><slot>© {{ year }}</slot></footer>
</template>

<style scoped>
.site-footer {
  background-color: #ffffff;
  color: #4A90E2;
  padding: 12px 16px;
  font-size: 14px;
  text-align: center;
  border-top: 2px solid #4A90E2;
  font-weight: 600;
}
</style>
