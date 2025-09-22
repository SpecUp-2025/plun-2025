<template>
  <div class="l-page">
    <div class="auth-wrap">
      <Logo/>
      <div class="auth-card">
        <form class="form" @submit.prevent="register">
          <div class="field">
            <label class="label" for="email">이메일</label>
            <div class="control with-button">
              <input v-model="form.email" type="email" @input="checkEmail"  id="email" 
              :disabled="clickVer" autocapitalize="off" autocomplete="email" placeholder="이메일을 입력해주세요" ></input>
              <button type="button" class="ver-btn" @click="sendCode" :disabled="clickVer || pendingSend">
                <span v-if="!pendingSend">인증하기</span>
                <span v-else class="spinner" aria-hidden="true"></span>
              </button>
            </div>   
            <div class="error-slot">
              <div class="error" v-show ="unvaild.email" v-text="unvaild.email"></div>
            </div>
          </div>
          <transition name="fade">
            <div v-if="showCode"  :class="['code-group', { 'code-group--verified': verified }]">
              <input v-model="form.code" type="text" maxlength="6" placeholder="인증번호 6자리" :disabled="verified" />
              <button v-if="!verified" type="button" class="ver-btn" @click="verifyCode":disabled="pendingVerify || (form.code?.length !== 6)">
                <span v-if="!pendingVerify">확인</span>
                <span v-else class="spinner" aria-hidden="true"></span>
              </button>
              <button v-if="!verified" type="button" class="ver-btn" @click="resend" :disabled="pendingSend || resendCooldown > 0">{{resendLabel}}</button>
              <span v-if="verified" class="ver">인증되었습니다</span>
            </div>
          </transition>
          <div class="field">
            <label class="label" for="password">비밀번호</label>
            <input id="password" v-model="form.password" type="password" placeholder="비밀번호를 입력해주세요."  autocomplete="new-password" autocapitalize="off" @input="checkPassword" />
            <div class="error-slot">
              <div class="error" v-show="unvaild.password" v-text="unvaild.password"></div>
            </div>
          </div>
          <div class="field">
            <label class="label" for="passwordCheck">비밀번호확인 </label>
            <input id="passwordCheck" v-model="passwordCheck" type="password" placeholder="비밀번호를 입력해주세요."  autocomplete="new-password" autocapitalize="off" @input="checkPasswordCheck" />
            <div class="error-slot">
              <div class="error" v-show="unvaild.passwordCheck" v-text="unvaild.passwordCheck"></div>
            </div>
          </div>
          <div class="field">
            <label class="label" for="name">이름</label>
            <input v-model="form.name" placeholder="이름을 입력해주세요" @input="checkName" id="name"></input>
            <div class="error-slot">
              <div class="error" v-show="unvaild.name" v-text="unvaild.name"></div>
            </div>
          </div>
          <button class="submit" type="submit" :disabled="!canSubmit" >회원가입</button> 
          <button class="cancel" type="button" @click="router.back()">취소</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import instance from '@/util/interceptors';
import { onBeforeUnmount, reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import '@/assets/css/auth.css'
import '@/assets/css/code.css'
import Logo from './Logo.vue';
import { REGEX_PATTERN } from '../util/Regex';

const router = useRouter();
const loginType =ref(0);
const form = reactive({
    email:'',
    password:'',
    name:'',
    code:'',
})
const passwordCheck = ref('')

const unvaild = reactive({
    email:'',
    password:'',
    name:'',
    passwordCheck:''
})

const touched = reactive({
    email:false,
    password:false,
    name:false,
    passwordCheck:false
})
const clickVer = ref(false) // 인증번호 누름 여부 
const showCode = ref(false) // 인증코드 입력칸 노출 여부
const verified = ref(false) // 이메일 인증 완료 여부
const pendingSend = ref(false);
const pendingVerify = ref(false);
const resendCooldown = ref(0);
let _resendTimer = null;

const resendLabel = computed(() =>
  resendCooldown.value > 0 ? `재전송 (${resendCooldown.value}s)` : '재전송'
);

function startCooldown(sec = 60) {
  clearInterval(_resendTimer);
  resendCooldown.value = sec;
  _resendTimer = setInterval(() => {
  resendCooldown.value -= 1;
  if (resendCooldown.value <= 0) {
    clearInterval(_resendTimer);
    _resendTimer = null;
  } 
  }, 1000);
}
onBeforeUnmount(() => { if (_resendTimer) clearInterval(_resendTimer); });

const canSubmit = computed(() => {
const noInvalid = !Object.values(unvaild).some(Boolean);
const filled =
form.email?.trim() &&
  form.password?.trim() &&
  form.name?.trim() &&
  (passwordCheck.value?.length ?? 0) > 0;
  return verified.value && noInvalid && !!filled;
});


const sendCode = async () => {
  const email = form.email?.trim().toLowerCase();
  if (!email) return alert('이메일을 입력하세요.')
  if(unvaild.email) return;
  pendingSend.value = true;
  try {
    const {status,data} = await instance.post(`/auth/email-code`,{email})
    if(status===200){
      loginType.value = data.type
      showCode.value = true
      clickVer.value = true
      unvaild.email = '';
      startCooldown(60)
      alert('인증번호를 보냈습니다.')
    }
  } 
  catch (error) {
    const sc  = error?.response?.status;
    if(sc ===409){
      unvaild.email = '이미 가입된 이메일입니다. 다른 이메일을 입력해주세요.';
      showCode.value = false;          
      verified.value = false;          
      clickVer.value = false; 
      return;         
    }
    alert('인증 코드 전송에 실패했습니다. 잠시 후 다시 시도해주세요.');
    console.error("인증 코드 전송 실패" ,error)
  }
  finally{
    pendingSend.value =false;
  }
}

const resend = async () => {
  if(resendCooldown.value>0)  return;
  await sendCode()
}

const verifyCode = async () => {
  const email = form.email?.trim().toLowerCase();
  if (!form.code) return alert('코드를 입력하세요.')
  pendingVerify.value = true
  try {
    const {status} = await instance.post(`/auth/verifyCode`,{
    email,
    code: form.code
  })
  if (status===200){
    verified.value = true
    clearInterval(_resendTimer)
    resendCooldown.value=0
    alert('이메일 인증 완료!')
}
  
  } 
  catch (error) {
    console.error('인증 실패', error)
    alert('이메일 인증 실패!')
  }
  finally{
    pendingVerify.value =false
  }
}

async function register() {
  checkEmail();
  checkPassword();
  checkPasswordCheck();
  checkName();
  if (Object.values(unvaild).some(v => !!v)) return;
  if (!verified.value) { alert('이메일 인증을 완료해주세요.'); return; }

  try {
    if(loginType.value ===2){
      const { status } = await instance.put('/auth/socialRegister', {
      email: form.email,
      password: form.password,
      name: form.name
    });
      if (status === 201) {
        alert('회원가입에 성공했습니다.');
        const hasAccess = !!localStorage.getItem('accessToken');
        router.push({ name: hasAccess ? 'teamList' : 'login' });
      }
    }
    else{
      const { status } = await instance.post('/auth/register', {
      email: form.email,
      password: form.password,
      name: form.name
    });
      if (status === 201) {
        alert('회원가입에 성공했습니다.');
        router.push({ name: 'login' });
      }
    }
} catch (e) {
  console.error('회원가입 실패', e);
  alert('회원가입에 실패했습니다.');
}
}

function checkEmail() {
  touched.email = true;

  if (!REGEX_PATTERN.EMAIL.test(form.email)) {
    unvaild.email = "올바르지 않은 이메일 형식입니다.";
    return;
  }
  unvaild.email = "";
}

function checkPassword() {
  touched.password = true;

  if (!REGEX_PATTERN.PASSWORD.test(form.password)) {
    unvaild.password = "비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&)를 포함해주세요.";
    return;
  }

  if (form.password.length < 8 || form.password.length > 20) {
    unvaild.password = "비밀번호 길이는 8~20자입니다.";
    return;
  }
  unvaild.password = "";
}

function checkPasswordCheck() {
  touched.passwordCheck = true;

  if (passwordCheck.value.length === 0) {
    unvaild.passwordCheck = "비밀번호 확인이 되지 않았습니다.";
    return;
  }
  if (passwordCheck.value !== form.password) {
    unvaild.passwordCheck = "비밀번호 확인이 일치하지 않습니다.";
    return;
  }
  unvaild.passwordCheck = "";
}

function checkName() {
  touched.name = true;

  if (form.name.length < 1 || form.name.length > 20) {
    unvaild.name = "이름 길이는 1~20자입니다.";
    return;
  }

  unvaild.name = "";
}
</script>