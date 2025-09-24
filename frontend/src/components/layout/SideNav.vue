<template>
  <aside class="side">
    <slot>side</slot>
  </aside>
</template>

<style>
.side {
  background: #D7E2EB;
  border-right: 2px solid rgba(0, 105, 217, 0.12);
  padding: 20px 16px;
  overflow: auto;
  color: #2d3748;
  font-weight: 500;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
  position: relative;
  min-width: 200px;
  max-width: 250px;
  display: flex;
  flex-direction: column;
  height: 88vh;
}
.side::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
}

.side a,
.side > button {
  display: block;
  width: 100%;
  text-align: center;
  padding: 5px 5px;
  margin-bottom: 6px;
  border: none;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  color: #153E75;
  font-size: 16px;
  font-weight: normal;
  border-radius: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  text-decoration: none;
  border: 1px solid rgba(255, 255, 255, 0.8);
  position: relative;
  overflow: hidden;
}

.side a::before,
.side button::before {
  content: none;
}

.side a:hover,
.side button:hover {
  background: rgba(0, 105, 217, 0.06);
  color: #0056b3;
  border-color: rgba(0, 105, 217, 0.15);
}

.side a:hover::before,
.side button:hover::before {
  left: 100%;
}

/* 활성 상태 */
.side a.router-link-active {
  background: linear-gradient(135deg, rgba(0, 105, 217, 0.15) 0%, rgba(74, 144, 226, 0.15) 100%);
  color: #0056b3;
  font-weight: normal;
  box-shadow: 0 4px 15px rgba(0, 105, 217, 0.2);
  border-color: rgba(0, 105, 217, 0.25);
  border-right: 3px solid #0069D9;
}

/* 스크롤바 스타일링 */
.side::-webkit-scrollbar {
  width: 6px;
}

.side::-webkit-scrollbar-track {
  background: transparent;
}

.side::-webkit-scrollbar-thumb {
  background: rgba(0, 105, 217, 0.3);
  border-radius: 3px;
}

.side::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 105, 217, 0.5);
}

/* 접근성 - 포커스 상태 */
.side a:focus,
.side button:focus {
  outline: 2px solid #0069D9;
  outline-offset: 2px;
}

/* 버튼 클릭 시 피드백 */
.side a:active,
.side button:active {
  transform: translateY(0px) scale(0.98);
  transition-duration: 0.1s;
}

/* 반응형 */
@media (max-width: 768px) {
  .side {
    padding: 16px 12px;
  }
  
  .side a,
  .side button {
    padding: 12px 14px;
    font-size: 15px;
  }
}

.side-main-button {
  flex-grow: 1;
  background: rgba(255, 255, 255, 0.7);
  color: #153E75;
  padding: 3px 3px;
  font-size: 16px;
  font-weight: normal;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  transition: all 0.25s ease;
  white-space: nowrap;
  margin-bottom: 8px;
  text-align: center;
}

.small-icon-button {
  padding: 3px 3px;
  font-size: 18px;
  line-height: 1;
  border-radius: 6px;
  width: auto !important;
  flex-shrink: 0;
  min-width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.8);
  color: #153E75;
  border: 1px solid rgba(255, 255, 255, 0.8);
  cursor: pointer;
  margin-bottom: 8px;
  text-align: center;
}

</style>