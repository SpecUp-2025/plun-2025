<template>
    <div class="modal-overlay" @click.self="close">
        <div class="modal" role="dialog" aria-modal="true" aria-labelledby="invTitle">
            <header class="modal-header">
                <h2 id="invTitle">초대 보관함</h2>
                <button class="icon-btn" @click="close" aria-label="닫기">×</button>
            </header>
        <div class="modal-body">
            <div v-if="!form.list.length" class="empty">초대 받은 알림이 없습니다.</div>
                <ul v-else class="invite-list">
                    <li v-for="item in form.list"  :key="item.invitedId" class="invite-item">
                        <div class="left">
                            <div class="avatar">{{ (item.teamName || '팀').slice(0,1) }}</div>
                            <div class="info">
                            <div class="name">{{ item.teamName }}</div>
                            <div class="meta">
                                <span class="label">초대한 사람 :</span>
                                <span class="val">{{ item.inviterName }}</span>
                                <span class="dot">.</span>
                                <span class="date">{{ formatDate(item.createDate) }}</span>
                            </div>
                            </div>
                        </div>
                
                        <div class="right">
                            <button class="btn ghost" @click="cancel(item.invitedId)">거절하기</button>
                            <button class="btn primary"  @click="accept(item.teamNo,item.invitedId)">수락하기</button>
                        </div>
                    </li>
                </ul>
        </div>
         <footer class="modal-footer">
            <button class="btn ghost"@click="close">닫기</button>
        </footer>
        </div>
    </div>
</template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter()
const userStore = useUserStore()
const userNo = computed(() => userStore.user?.userNo ?? '')
const form = reactive({
    list : [],
})
const formatDate = (s) => s ? s.replace('T', ' ').slice(0, 16) : '';
const close = () => router.back()
const onEsc = e => { if (e.key === 'Escape') close() }

onMounted(async () => {
    await invitation()
    const prev = document.body.style.overflow
    document.body.dataset.prevOverflow = prev
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onEsc)
})
onBeforeUnmount(() => {
    document.body.style.overflow = document.body.dataset.prevOverflow || ''
    window.removeEventListener('keydown', onEsc)
})

const invitation = async () =>{
    console.log(userNo.value)
    try {
        const { status, data} = await instance.get(`/teams/invitation/${userNo.value}`)
        console.log("data",data)
        if(status ===200 ){
            form.list = data
        }
        console.log(form.list)
        
    } 
    catch (error) {
        console.error("리스트를 불러오는데 실패했습니다.",error)
    }
}

const accept = async (teamNo,invitedId) =>{
    try {
        const {status} = await instance.post(`/teams/accept`,{
            teamNo,
            invitedId,
            userNo: userNo.value
        })
        if(status === 204){
            alert("수락 했습니다.")
            await invitation()
        }
    } 
    catch (error) {
        console.error("수락 실패",error)
    }
}
const cancel = async (invitedId) =>{
    try {
        const {status} = await instance.put(`/teams/cancel/${invitedId}`)
        if(status === 204){
            alert("거절 했습니다.")
            await invitation()
        }
    } 
    catch (error) {
        console.error("거절 실패",error)
    }
}
</script>

<style scoped>
.modal-overlay{
  position: fixed; inset: 0;
  display: grid; place-items: start center;
  padding-top:22vh;
  background: rgba(0,0,0,.35);
  z-index: 1000;
}

.modal{
  width: 100%;
  max-width: 720px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,.25);
  overflow: hidden;
  max-height: 90dvh;
  overflow: auto;
  animation: pop .16s ease;
}
@keyframes pop{ from{ transform: translateY(8px); opacity:0 } to{ transform: translateY(0); opacity:1 } }

.modal-header{
  display:flex; align-items:center; justify-content:space-between;
  padding:14px 16px; border-bottom:1px solid #eef0f3;
}
.modal-header h2{ margin:0; font-size:18px; }
.icon-btn{ background:transparent; border:0; cursor:pointer; font-size:22px; padding:2px 6px; border-radius:8px; }
.icon-btn:hover{ background:#f3f4f6; }

.modal-body{ padding:16px; display:grid; gap:12px; }
.empty{
  background:#fff; border:1px dashed #e5e7eb; border-radius:12px;
  padding:18px; text-align:center; color:#6b7280;
}

.invite-list{ list-style:none; margin:0; padding:0; display:grid; gap:10px; }
.invite-item{
  display:flex; align-items:center; justify-content:space-between;
  background:#fff; border:1px solid #eef0f3; border-radius:12px;
  padding:12px 14px;
}
.left{ display:flex; align-items:center; gap:10px; min-width:0; }
.avatar{
  width:32px; height:32px; border-radius:50%;
  background:#e8f0ff; color:#1d4ed8; font-weight:700; display:grid; place-items:center;
}
.info{ min-width:0; }
.name{ font-weight:600; }
.meta{ font-size:12px; color:#6b7280; display:flex; gap:6px; flex-wrap:wrap; }
.label{ color:#9ca3af; }
.dot{ color:#d1d5db; }
.right{ display:flex; align-items:center; gap:8px; }

.modal-footer{
  display:flex; justify-content:flex-end; gap:8px;
  padding:12px 16px; border-top:1px solid #eef0f3;
}
</style>