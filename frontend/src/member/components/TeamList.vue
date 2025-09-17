<template>
  <HomeHeader/>
  <main class="page">
    <div class="container">
      <section class="profile">
        <div class="avatar">{{ (name || '').slice(0,1) }}</div>
        <div class="who">
          <h2 class="who__name">{{ name }}</h2>
          <p class="who__email">{{ email }}</p>
        </div>
        <div class="profile__actions">
          <button class="btn ghost" @click="router.push({name : 'detail'})">계정 설정</button>
          <button class="btn ghost" @click="router.push({name: 'invitation'})">초대보관함</button>
        </div>
      </section>

      <h3 class="section-title">팀 리스트</h3>
      <div v-if="!team.list.length" class="empty">
        팀 리스트가 없습니다.
      </div>
      <ul v-else class="team-list">
        <li v-for="(item,index) in team.list" :key="item.teamNo" class="team-item">
          <div class="item-left">
            <div class="badge">
              {{ (item.teamName || '').slice(0,1) }}
            </div>
            <div class="team">
              <div class="team__name">
                {{ item.teamName }}
              </div>
              <div class="team__meta">
                <span class="date">
                  {{ item.createDate }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="item-right">
            <button class="btn ghost" @click = "router.push({name:'setting',
              params:{teamNo:item.teamNo}})">
              설정하기
            </button>
            <button class="btn primary" @click = "router.push({name : 'teamMain', 
              params:{ teamNo: item.teamNo }})">
              입장하기</button>
          </div>
        </li>
      </ul>
      <div class="footer-actions">
        <button class="btn outline outline--xl"  @click="router.push({name:'teamCreate'})">+ 팀 추가하기</button>
      </div>
      <RouterView/>
    </div>
  </main>
</template>

<script setup>
import HomeHeader from '@/main/HomeHeader.vue';
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive } from 'vue';
import { RouterView, useRouter } from 'vue-router';
import '@/assets/css/home.css'

const userStore = useUserStore()
const router = useRouter()
const email = computed(() => userStore.user?.email ?? '');
const name = computed(() => userStore.user?.name ?? '');
const userNo = computed(() => userStore.user?.userNo ?? '');
const team = reactive({
  list:[],
})
onMounted(async ()=>{
  await teamList()
}) 
const teamList = async ()=>{
  try {
    const {status , data} = await instance.get(`/teams/teamList/${userNo.value}`)
    if(status===200){
      team.list = data
    }
  } catch (error) {
    console.error("리스트를 불러오는데 실패했습니다.",error)
  }
}
</script>
<style scoped>
.profile{
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 16px;
  padding: 35px 16px;
  box-shadow: 0 8px 24px rgba(0,0,0,.04);
  margin-bottom: 16px;
}
.avatar{
  width: 36px; height: 36px; border-radius: 50%;
  background: #e8f0ff; color:#1d4ed8; font-weight: 700;
  display:grid; place-items:center;
}

.profile__actions{ margin-left:auto; display:flex; gap:8px; }

.section-title{ margin: 10px 0; font-size: 14px; color:#6b7280; }

.empty{
  background:#fff;
  border:1px dashed #e5e7eb;
  border-radius:12px;
  padding: 18px;
  text-align:center;
  color:#6b7280;
}

.team-list{ list-style:none; padding:0; margin:0; display:grid; gap:15px; }
.team-item{
  display:flex; align-items:center; justify-content:space-between;
  background:#fff; border:3px solid #eef0f3; border-radius:12px;
  padding: 16px 20px; 
}
.item-left{ display:flex; align-items:center; gap:10px; min-width:0; }
.badge{
  width:40px; height:40px; border-radius:8px;
  background:#f3f4f6; color:#6b7280; font-size:12px;
  display:grid; place-items:center;
}
.team__name{ font-weight: 600; font-size: 18px;}
.team__meta{ font-size:11px; color:#6b7280; display:flex; gap:6px; }
.item-right{ display:flex; align-items:center; gap:8px; }

.btn.outline{ width:100%; border:1px dashed #d1d5db; background:#fff; padding:12px; }
.outline--xl{
  padding: 18px 22px;      
  min-height: 56px;        
  font-size: 16px;         
  border-width: 2px;   
  border-radius: 12px;
}
.footer-actions{ display:grid; gap:10px; margin-top:16px; }

@media (min-width: 980px){
  .container{ max-width: 920px; }
}
</style>
