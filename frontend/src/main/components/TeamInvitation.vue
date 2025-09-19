<template>
    <div class="modal-overlay" @click.self="close">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <header class="modal-header">
          <h2 id="modalTitle">팀만들기</h2>
          <button class="icon-btn" @click="close()"aria-label="닫기">×</button>
        </header>
        <div v-if="pending" class="busy">
          <div class="spinner" aria-hidden="true"></div>
        </div>
        <div class="modal-body">
          <label class="label" for="inviteEmail">초대 이메일</label>
          <div class="row">
            <input v-model.trim="inputEmail" type="email" 
              placeholder="example@example.com" 
              @keydown.enter.prevent="add"
              @keydown.space.prevent="add"
              @keyup="onComma"/>
            <button type="button" class="btn ghost" @click="add" :disabled="!inputEmail">추가</button>
          </div>
          <div v-show ="unEmailVaild" class="error" v-text="unEmailVaild"></div>
        
          <div v-if="form.invite.length" class="chips">
            <span v-for="(e,i) in form.invite" :key="e" class="chip">
              {{ e }}
              <button class="chip__remove" @click="remove(i)" aria-label="삭제">×</button>
            </span>
          </div>
        </div>
        <footer class="modal-footer">
            <button class="btn ghost" @click="close">취소</button>
            <button class="btn primary" @click="create">초대하기</button>
        </footer>
      </div>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import instance from '@/util/interceptors';
import { useUserStore } from '@/store/userStore';
import '@/assets/css/home.css'
import { REGEX_PATTERN } from '@/member/util/Regex';

const props = defineProps({
  teamNo: { type: String, required: true }
})
const pending = ref(false); 
const router = useRouter()
const userStore = useUserStore();
const userNo = computed(() => userStore.user?.userNo ?? '')
const userEmail = computed(() => userStore.user?.email ?? '')
const userName = computed(() => userStore.user?.name ?? '')
const form = reactive({
    invite:[],
})
const inputEmail = ref('')
const unEmailVaild = ref('')
const close = () => router.back()
const onEsc = e => { if (e.key === 'Escape') close() }

onMounted(() => {
  const prev = document.body.style.overflow;
  document.body.dataset.prevOverflow = prev;
  document.body.style.overflow = 'hidden';
  window.addEventListener('keydown', onEsc);
});
onBeforeUnmount(() => {
  document.body.style.overflow = document.body.dataset.prevOverflow || '';
  window.removeEventListener('keydown', onEsc);
});
const create = async () => {
    if (pending.value) return; 
    const email  = (userEmail.value ?? '').trim().toLowerCase()
    pending.value = true;
    try {
        const {status,data} = await instance.post(`teams/memberInvite`,{
            userNo : userNo.value,
            teamNo : props.teamNo,
            invite: form.invite,
            email,
            userName:userName.value

        })
        if(status ===200){
            alert("초대했습니다.")
            close()
        }
    } 
    catch (error) {
        console.error('초대하지 못했습니다.',error);
        alert("초대하지 못했습니다.")
    }
    finally {
      pending.value = false;
  }
}
function addEmail(){
    const email = inputEmail.value.trim().toLowerCase();
    const me  = (userEmail.value ?? '').trim().toLowerCase()
    if(!email) return;
    if(!REGEX_PATTERN.EMAIL.test(email)){
        unEmailVaild.value = '올바르지 않은 이메일 형식입니다.'
        return false;
    }
    if(email === me){
        unEmailVaild.value = '본인의 이메일은 추가 못합니다.'
        return false;
    }
    unEmailVaild.value='';
    if(!form.invite.includes(email)) form.invite.push(email)
    return true;
}

function add(){
    if (pending.value) return;
    if (!inputEmail.value) return
    const ok = addEmail()  
    if (ok) inputEmail.value = ''
}

function remove(i){
    form.invite.splice(i,1)
}
function onComma (e) {
  if (e.key === ',') {
    addEmail()
    e.preventDefault()
  }
}
</script>

<style scoped>
.modal-overlay{
  position: fixed; inset: 0;
  background: rgba(0,0,0,.35);
  display: grid; 
  place-items: start center;
  padding: 24px;
  padding-top:22vh;
  z-index: 1000;
}

.modal{
  width: 100%;
  max-width: 560px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,.25);
  overflow: hidden;
  animation: pop .16s ease;
  position: relative;
}

@keyframes pop{
  from{ transform: translateY(8px); opacity: 0; }
  to{ transform: translateY(0); opacity: 1; }
}

.modal-header{
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef0f3;
}
.modal-header h2{ margin: 0; font-size: 18px; }

.icon-btn{
  background: transparent; border: 0; cursor: pointer;
  font-size: 22px; line-height: 1; padding: 2px 6px; border-radius: 8px;
}
.icon-btn:hover{ background: #f3f4f6; }

.modal-body{
  padding: 16px;
  display: grid; gap: 10px;
}
.label{ font-size: 13px; color:#6b7280; }

.row{ display: grid; grid-template-columns: 1fr auto; gap: 8px; }

input{
  height: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0 12px;
  outline: none;
}
input:focus{ border-color: #9ca3af; }

.chips{ display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.chip{
  background: #f3f4f6; border-radius: 999px; padding: 6px 10px;
  display: inline-flex; gap: 6px; align-items: center; font-size: 13px;
}
.chip__remove{
  border: 0; background: transparent; cursor: pointer; font-size: 16px; line-height: 1;
}

.error{ color: #d93025; font-size: 12px; margin: 0; }

.busy{
  position:absolute; inset:0;
  background: rgba(255,255,255,.7);
  display:grid; place-items:center;
  gap:10px;
  text-align:center;
  z-index: 2;
  pointer-events: all;
  border-radius: 16px;
}

.spinner{
  width: 28px; height: 28px;
  border-radius: 50%;
  border: 3px solid #e5e7eb;
  border-top-color: #10b981;
  animation: spin 0.9s linear infinite;
}
.modal-footer{
  display: flex; gap: 8px; justify-content: flex-end;
  padding: 12px 16px; border-top: 1px solid #eef0f3;
}

.btn:disabled{ opacity:.6; cursor:not-allowed; }
</style>