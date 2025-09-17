<template>
    <HomeHeader/>

    <main class="page">
        <div class="container">
            <section class="card">
                <header class="team-head">
                    <div class="avatar">{{ (form.list.teamName || '팀').slice(0,1) }}</div>
                    <div class="who">
                        <h2 class="team__name">{{form.list.teamName}}</h2>
                        <p class="team__meta">
                        {{ formatDate(form.list.createDate) }}
                        </p>
                    </div>
                    <button class="btn primary" @click = "router.push({name : 'teamMain', params:{ teamNo: item.teamNo }})">입장하기</button>
                </header>

                <details ref="delDetail" class="block danger">
          <summary class="summary">팀 탈퇴</summary>
          <div class="panel">
            <div class="warn">
              탈퇴 시 이 팀에서의 권한과 데이터 접근이 제거됩니다. 계속하려면 <b>탈퇴</b> 라고 입력하세요.
            </div>
            <input
              v-model.trim="confirmText"
              placeholder="탈퇴 라고 입력하세요"
            />
            <div class="actions">
              <button class="btn ghost" type="button" @click="onCancelDelete">취소</button>
              <button
                class="btn danger-btn"
                :disabled="confirmText !== '탈퇴'"
                @click="teamDelete"
              >
                탈퇴하기
              </button>
            </div>
          </div>
        </details>
        <div class="footer-back">
            <button class="btn ghost" @click="router.back()">뒤로가기</button>
        </div>
        </section>
</div>
    </main>
    
        
        
</template>

<script setup>
import HomeHeader from '@/main/HomeHeader.vue';
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import '@/assets/css/home.css'

const route = useRoute()
const router = useRouter()
const form = reactive({
    list:{}
})
const confirmText = ref('')
const delTeam = ref(null)

const userStore = useUserStore()
const userNo = computed(() => userStore.user?.userNo ?? '')
onMounted( async () => {
    await teamDetail()
})
const formatDate = (s) => s ? s.replace('T', ' ').slice(0, 16) : '';
const teamDetail = async () => {
    try {
        const {status,data} = await instance.get(`/teams/teamDetail/${route.params.teamNo}`)
        if(status ===200)    {
            form.list = data.list
        }
    } catch (error) {
        console.error("팀 정보 확인 실패",error)
    }
    
}
function onCancelDelete() {
  if (delTeam.value) delTeam.value.open = false;
  confirmText.value = '';
}
const teamDelete = async () => {
    try {
        const {status} = await instance.put(`/teams/teamDelete`,{
            teamNo : route.params.teamNo,
            userNo : userNo.value
        })
        if(status ===200)    {
            alert('팀 탈퇴 완료')
            router.push({name:'teamList'})
        }
    } catch (error) {
        console.error("탈퇴실패",error)
    }
}
</script>

<style scoped>
.container{
  max-width:840px;
}

.card{
  background:#fff; 
  border:1px solid #eef0f3; 
  border-radius:16px;
  margin-top: 40px;
  box-shadow:0 8px 24px rgba(0,0,0,.04); 
  padding:16px; 
  display:grid; 
  gap:16px;
}

.team-head{ display:flex; align-items:center; gap:12px; }
.avatar{
  width:40px; height:40px; border-radius:50%; display:grid; place-items:center;
  background:#e8f0ff; color:#1d4ed8; font-weight:700;
}
.who{ min-width:0; }
.team__name{ margin:0; font-weight:700; }
.team__meta{ margin:2px 0 0; color:#6b7280; font-size:12px; }
.dot{ margin:0 6px; color:#d1d5db; }
.team-head .btn{ margin-left:auto; }

.list{ margin:0; }
.row{
  display:flex; align-items:center; justify-content:space-between;
  padding:10px 4px; border-top:1px solid #f1f3f5;
}
.row:first-child{ border-top:0; }
dt{ color:#6b7280; }
dd{ margin:0; font-weight:600; }

.block{ border-top:1px solid #f1f3f5; padding-top:8px; }
.summary{
  display:flex; align-items:center; justify-content:space-between;
  padding:8px 0; cursor:pointer; list-style:none;
}
.summary::-webkit-details-marker{ display:none; }
.summary::after{ content:"▸"; opacity:.4; transition:.15s; }
details[open] .summary::after{ transform:rotate(90deg); }

.panel{ display:grid; gap:8px; padding:4px 0 8px; }
.warn{
  background:#fff8f2; border:1px solid #ffd8b5; color:#b45309;
  padding:10px; border-radius:10px;
}
input{
  height:40px; border:1px solid #e5e7eb; border-radius:10px; padding:0 12px; outline:none;
}
input:focus{ border-color:#9ca3af; }

.actions{ display:flex; gap:8px; justify-content:flex-end; margin-top:6px; }
.footer-back{ display:flex; justify-content:flex-end; margin-top:4px; }

</style>