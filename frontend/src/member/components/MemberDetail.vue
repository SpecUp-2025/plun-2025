<template>
  <HomeHeader/>
  <main class="page">
    <div class="container">
      <section class="card">
        <dl class="list">
          <div class="row">
            <dt>이름</dt>
            <dd>{{name}}</dd>
          </div>
          <div class="row">
            <dt>이메일</dt>
            <dd>{{ email }}</dd>
          </div>
        </dl>
        <details ref="pwdDetail" class="block">
          <summary class="summary">
            <span>비밀번호 변경</span>
            <span class="muted" >최근 변경 이력 : {{ formatDate(form.date.updateDate) }}</span>
          </summary>
          <div class="panel" v-if="form.date.loginTypeNo == 2">
            <div class="tip">
              소셜로그인 계정입니다 비밀번호 변경시에는 회원가입을 해주세요.
            </div>
            <button class="btn ghost" @click="router.push({ name: 'register' })">회원가입하러가기</button>
          </div>
          <div class="panel" v-else>
            <label class="label">현재 비밀번호</label>
            <input v-model="form.currentPassword" type="password" placeholder="현재 비밀번호" />
            
            <label class="label">새 비밀번호</label>
            <input v-model="form.password" type="password" placeholder="새 비밀번호" @input="checkPassword" />
            <div class="error-slot">
              <div class="error" v-show="unvaild.password" v-text="unvaild.password"></div>
            </div>

            <label class="label">비밀번호 확인</label>
            <input v-model="passwordCheck" type="password" placeholder="비밀번호 확인" @input="checkPasswordCheck" />
            <div class="error-slot">
              <div class="error" v-show="unvaild.passwordCheck" v-text="unvaild.passwordCheck"></div>
            </div>
            <div class="actions">
              <button class="btn ghost" type="button" @click="onCancelPwd">취소</button>
              <button class="btn primary" @click="sendPassword">확인</button>
            </div>
          </div>
        </details>
  
        <details ref="delDetail" class="block danger" >
          <summary class="summary" >계정 탈퇴</summary>
          <div class="panel">
            <div class="warn">계정 탈퇴시 기존 데이터는 삭제 됩니다.</div>
            <div class="field">
              <label class="label" for="confirmDel"></label>
              <input id="confirmDel" v-model.trim="confirmText" placeholder="탈퇴 라고 입력하세요"
                @keyup.enter="isConfirmOk && userDelete()" />
              <p class="help" :class="{ ok: isConfirmOk }">
                "탈퇴"라고 정확히 입력해야 삭제됩니다.
              </p>
            </div>
            <div class="actions">
              <button class="btn danger-btn" :disabled="confirmText !== '탈퇴'" @click="userDelete">계정 삭제</button>
              <button class="btn ghost" type="button" @click="onCancelDelete">취소</button>
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
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive, ref } from 'vue';
import { REGEX_PATTERN } from '../util/Regex';
import { useRouter } from 'vue-router';
import HomeHeader from '@/main/HomeHeader.vue';
import '@/assets/css/home.css'

const router = useRouter();
const userStore = useUserStore();
const email = computed(() => userStore.user.email ?? '');
const name = computed(() => userStore.user.name ?? '');
const userNo = computed(() => userStore.user.userNo ?? '');
const formatDate = (s) => s ? s.replace('T', ' ').slice(0, 16) : '';

const form = reactive({
  date: {},
  password: '',
  currentPassword: '',
});
const confirmText = ref('');
const isConfirmOk = computed(() => confirmText.value === '탈퇴');

const passwordCheck = ref('');
const pwdDetail = ref(null)
const delDetail = ref(null)

const unvaild = reactive({
  password: '',
  passwordCheck: ''
});

const touched = reactive({
  password: false,
  passwordCheck: false
});

onMounted(() => {
  dataList();
});

function resetPwdFields() {
form.currentPassword = '';
  form.password = '';
  passwordCheck.value = '';
  unvaild.password = '';
  unvaild.passwordCheck = '';
}

function onCancelPwd() {
  resetPwdFields();
  if (pwdDetail.value) pwdDetail.value.open = false;
}

function onCancelDelete() {
  if (delDetail.value) delDetail.value.open = false;
  confirmText.value = '';
}


const userDelete = async () =>{
  if (!isConfirmOk.value) return; 
    try {
        const {status,data} = await instance.put(`member/userDelete/${userNo.value}`)
        if(status ===200 && data.success){
            alert("회원 탈퇴 완료")
            router.push({name:'login'})
        } else {
            alert('삭제 실패');
  }
    } catch (error) {
        console.error('계정 탈퇴 실패',error)
    }
}

const dataList = async () => {
  try {
    const { status, data } = await instance.get(`/member/me`);
    if (status === 200) {
      form.date = data;
    }
  } catch (error) {
    console.error('회원 정보 확인 실패', error);
  }
};

const sendPassword = async () => {
  checkPassword();
  checkPasswordCheck();
  if (Object.values(unvaild).some(v => !!v)) return;

  try {
    const { status } = await instance.post('/member/setPassword', {
      email: email.value,
      password: form.password,
      currentPassword: form.currentPassword
    });
    if (status === 200) {
      alert('비밀번호 재설정에 성공했습니다. 로그인 화면으로 이동합니다.');
      router.push({ name: 'login' });
    }
  } catch (e) {
    const status = e?.response?.status;
    const msg = e?.response?.data?.message;
    if (status === 400) {
      alert(msg);
      return;
    }

    console.error('비밀번호 재설정 실패', e);
  }
};

function checkPassword() {
  touched.password = true;

  if (!REGEX_PATTERN.PASSWORD.test(form.password)) {
    unvaild.password = '비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&)를 포함해주세요.';
    return;
  }

  if (form.password.length < 8 || form.password.length > 20) {
    unvaild.password = '비밀번호 길이는 8~20자입니다.';
    return;
  }

  unvaild.password = '';
}

function checkPasswordCheck() {
  touched.passwordCheck = true;

  if (passwordCheck.value.length === 0) {
    unvaild.passwordCheck = '비밀번호 확인이 되지 않았습니다.';
    return;
  }

  if (passwordCheck.value !== form.password) {
    unvaild.passwordCheck = '비밀번호 확인이 일치하지 않습니다.';
    return;
  }

  unvaild.passwordCheck = '';
}
</script>
<style scoped>
.container{
  max-width:840px;
}
.card{
  background:#fff; border:1px solid #eef0f3; border-radius:16px;
  box-shadow:0 8px 24px rgba(0,0,0,.04);
  padding:16px; display:grid; gap:16px;
  margin-top: 40px;
}

.profile-head{ display:flex; align-items:center; gap:12px; }
.avatar-lg{ width:48px; height:48px; border-radius:50%; display:grid; place-items:center;
  background:#e8f0ff; color:#1d4ed8; font-weight:700; font-size:18px; }

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
.muted{ color:#9ca3af; font-size:12px; }

.panel{ display:grid; gap:8px; padding:4px 0 8px; }
.label{ font-size:13px; color:#6b7280; }
.tip{ background:#f1f5ff; border:1px solid #dbe7ff; color:#1e40af;
  padding:10px; border-radius:10px; }
.warn{ background:#fff8f2; border:1px solid #ffd8b5; color:#b45309;
  padding:10px; border-radius:10px; }

input{
  height:40px; border:1px solid #e5e7eb; border-radius:10px; padding:0 12px; outline:none;
}
input:focus{ border-color:#9ca3af; }
.error{ color:#d93025; font-size:12px; margin:0; }
.error-slot { 
  min-height: 16px; padding-top: 2px;
}

.actions{ display:flex; gap:8px; justify-content:flex-end; margin-top:6px; }
.footer-back{ display:flex; justify-content:flex-end; margin-top:4px; }


.field input{ width:100%; height:40px; border:1px solid #e5e7eb; border-radius:10px; padding:0 12px; }
.field input:focus{ border-color:#9ca3af; }
.help{ font-size:12px; color:#9ca3af; margin:4px 0 0; }
.help.ok{ color:#10b981; }
.btn:disabled{ opacity:.6; cursor:not-allowed; }

</style>