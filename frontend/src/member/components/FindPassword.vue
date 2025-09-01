<template>
    <form @submit.prevent="reset">
    <label for="email">이메일</label>
    <input v-model="form.email" type="email" @input="checkEmail"  id="email" :disabled="clickVer" autocapitalize="off" autocomplete="email"
    placeholder="이메일을 입력해주세요" ></input>

    <div v-show ="unvaild.email" v-text="unvaild.email"></div>
    
    <button type="button" @click="sendCode" :disabled="clickVer">인증하기</button>
    
    <transition name="fade">
      <div v-if="showCode" style="margin-top:8px;">
        <input
          v-model="form.code"
          type="text"
          maxlength="6"
          placeholder="인증번호 6자리"
          autocomplete="one-time-code"
          :disabled="verified" />
        <button type="button" @click="verifyCode" :disabled="verified">확인</button>
        <button type="button" @click="resend" :disabled=" verified">재전송</button>
        <span v-if="verified" style="margin-left:8px;color:#22c55e;">인증되었습니다</span>
      </div>
    </transition>

    <br/>
    <transition name="fade">
      <div v-if="verified" style="margin-top:12px;">
        <label for="password">새 비밀번호</label>
        <input
          id="password"
          v-model="form.password"
          type="password"
          placeholder="비밀번호를 입력해주세요."
          autocomplete="new-password"
          autocapitalize="off"
          @input="checkPassword"
        />
        <div v-show="unvaild.password" v-text="unvaild.password"></div>

        <label for="passwordCheck">새 비밀번호 확인</label>
        <input
          id="passwordCheck"
          v-model="passwordCheck"
          type="password"
          placeholder="비밀번호를 입력해주세요."
          autocomplete="new-password"
          autocapitalize="off"
          @input="checkPasswordCheck"
        />
        <div v-show="unvaild.passwordCheck" v-text="unvaild.passwordCheck"></div>

        <button type="submit">비밀번호 재설정</button>
      </div>
    </transition>
    <RouterLink :to="{ name: 'login' }">취소</RouterLink>

    </form>
</template>

<script setup>
import instance from '@/util/interceptors';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { REGEX_PATTERN } from '../util/Regex';

const router = useRouter();

const form = reactive({
    email:'',
    password:'',
    code:'',
})
const passwordCheck = ref('')

const unvaild = reactive({
    email:'',
    password:'',
    passwordCheck:''
})

const touched = reactive({
    email:false,
    password:false,
    passwordCheck:false
})
const clickVer = ref(false) // 인증번호 누름 여부 
const showCode = ref(false) // 인증코드 입력칸 노출 여부
const verified = ref(false) // 이메일 인증 완료 여부



const sendCode = async () => {
    const email = form.email?.trim().toLowerCase();
    if (!email) return alert('이메일을 입력하세요.')
    try {
        const {status} = await instance.post(`/auth/reset-email-code`,{email})
        if(status===200){
            showCode.value = true
            clickVer.value = true
            unvaild.email = '';
            alert('인증번호를 보냈습니다.')
        }
    } catch (error) {
        const sc  = error?.response?.status;
        if(sc ===409){
            unvaild.email = '가입된 메일이 없습니다. 다른 이메일을 입력해주세요.';
            showCode.value = false;          
            verified.value = false;          
            clickVer.value = false; 
            return;         
        }
        alert('인증 코드 전송에 실패했습니다. 잠시 후 다시 시도해주세요.');
        console.error("인증 코드 전송 실패" ,error)
    }
}

const resend = async () => {
    await sendCode()
}

const verifyCode = async () => {
    const email = form.email?.trim().toLowerCase();
    if (!form.code) return alert('코드를 입력하세요.')
    try {
        const {status} = await instance.post(`/auth/verifyCode`,{
        email,
        code: form.code
    })
    if (status===200){
        verified.value = true
        alert('이메일 인증 완료!')
    }
    
    
    } catch (error) {
        console.error('인증 실패', error)
        alert('이메일 인증 실패!')
    }
}

async function reset() {
    checkEmail();
    checkPassword();
    checkPasswordCheck();
    if (Object.values(unvaild).some(v => !!v)) return;
    if (!verified.value) { alert('이메일 인증을 완료해주세요.'); return; }

    try {
    const { status } = await instance.post('/auth/resetPassword', {
      email: form.email,
      password: form.password,
    });
    if (status === 201) {
      alert('비밀번호 재설정에 성공했습니다.');
      router.push({ name: 'login' });
    }
  } catch (e) {
    console.error('비밀번호 재설정 실패', e);
    alert('비밀번호 재설정에 실패했습니다.');
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

</script>

<style lang="scss" scoped>

</style>