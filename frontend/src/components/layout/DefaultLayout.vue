<script setup>
import HeaderBar from './HeaderBar.vue'
import SideNav from './SideNav.vue'
import SiteFooter from './SiteFooter.vue'
</script>

<template>
  <div class="wrap">
    <HeaderBar><slot name="header"/></HeaderBar>
    <div class="body">
      <SideNav><slot name="sidebar"/></SideNav>
      <main class="main"><slot/></main>
    </div>
    <SiteFooter><slot name="footer"/></SiteFooter>
  </div>
</template>

<style scoped>
.wrap{display:grid;grid-template-rows:auto 1fr auto;height:100vh;background:#fff;}
.body{display:grid;grid-template-columns:220px 1fr;min-height:0;}
.main{padding:16px;overflow:auto;}
</style>
