<template>
  <aside class="side">
    <slot>side</slot>
  </aside>
</template>

<style>
.side {
  background-color: 
#e9eef5;
  border-right: 1px solid 
#c5d4e7;
  padding: 12px;
  overflow: auto;
  color: 
#2c3e50;
  font-weight: 550;
}
.side a,
.side button {
  display: block;
  width: auto;
  text-align: left;
  padding: 0;
  margin-bottom: 8px;
  border: none;
  background-color: transparent;
  color: 
#0069D9;
  font-size: 20px;
  font-weight: 800;
  border-radius: 4px;
  transition: background-color 0.2s ease, color 0.2s ease;
  cursor: pointer;
  text-decoration: none;
}

.side a:hover,
.side button:hover {
  background-color: 
#e0efff;
  color: 
#4A90E2;
}

.side a.router-link-active {
  background-color: 
#d0e7ff;
  color: 
#4A90E2;
  font-weight: 600;
}
</style>