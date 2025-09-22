<template>
  <div class="l-page">
    <div class="auth-wrap">
      <Logo/>
      <div class="auth-card">
        <form class="form" @submit.prevent="login">
          <div class="field">
            <label class="label" for="email">이메일</label>
            <input v-model="form.email" type="email" name="email" 
                autocomplete="email" placeholder="이메일을 입력해주세요" @input="checkEmail" />
            <div class="error-slot">
              <div class="error" v-show ="unvaild.email" v-text="unvaild.email"></div> 
            </div>
          </div>
          <div class="field">
            <label class="label" for="password">비밀번호</label>
            <input v-model="form.password" type="password" name="password"
                autocomplete="current-password" placeholder="비밀번호를 입력해주세요" />
          </div>
          <button class="submit"type="submit" >로그인</button>
        </form>
        <div class="divider"></div>
        <div class="oauth-buttons">
          <button class="oauth oauth--google"  @click="socialLogin('google')">
            <span class="oauth__brand">Google</span><span class="oauth__with"> 로그인</span>
          </button>
          <button class="oauth oauth--naver" @click="socialLogin('naver')">
            <span class="oauth__brand">NAVER</span><span class="oauth__with"> 로그인</span>
          </button>
          <button class="oauth oauth--kakao" @click="socialLogin('kakao')">
            <span class="oauth__brand">Kakao</span><span class="oauth__with"> 로그인</span>
          </button>
        </div>
        <div class="links">
          <RouterLink :to="{name : 'register'}" >회원가입</RouterLink>
          <RouterLink :to="{name : 'find'}" >비밀번호 찾기</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
  
  <script setup>
import { REGEX_PATTERN } from '@/member/util/Regex'
import { getUser } from '@/util/getUser'
import axios from 'axios'
import { reactive, } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import '@/assets/css/auth.css'
import Logo from '@/member/components/Logo.vue'

const router = useRouter()
const form = reactive({ email: '', password: '' })

const unvaild = reactive({
  email:'',
})

const login = async () => {
  checkEmail();
  if (Object.values(unvaild).some(v => !!v)) return;
  const email = form.email?.trim().toLowerCase();
  if (!email || !form.password) return alert('이메일/비밀번호를 입력하세요.')
  try {
    const { data,status } = await axios.post(`api/auth/login`, {
      email,
      password: form.password,
    })
    if(status ===200){
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await getUser()
      alert('로그인 성공입니다.')
      await router.push({ name: 'teamList' })
    }
    
  } catch (error) {
    console.error('로그인 실패', error)
    alert('이메일/비밀번호를 확인해주세요')
  }
}


function checkEmail() {
  if (!REGEX_PATTERN.EMAIL.test(form.email)) {
    unvaild.email = "올바르지 않은 이메일 형식입니다.";
    return;
  }
  unvaild.email = "";
}

function socialLogin(name) {
  window.location.href = `/api/oauth/${name}/login`

}
</script>
<style scoped>

.submit {
  width: 100%;
  border: 0;
  color: #fff;
  background-image: linear-gradient(90deg, #575758, #1f1e1f);
  box-shadow: 0 8px 24px rgba(103, 100, 109, 0.25);
  height: 44px;
  border-radius: 12px;
  font-size: 14px;
  cursor: pointer;
}

.submit:hover { filter: brightness(1.02); }

.divider {
  text-align: center;
  color: #434343;
  font-size: 12px;
  margin: 14px 0;
}

.oauth-buttons {
  display: grid;
  gap: 8px;
}
.oauth {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
}

.oauth--google {
  background: #fff;
  color: #1f2937;              
  border-color: #d1d5db;  
}
.oauth--google:hover { background: #f9fafb; }
.oauth__brand{ font-weight:700; letter-spacing:.2px; }
.oauth__with{ opacity:.85; font-weight:500; }

.oauth--naver {
  background: #03C75A;
  color: #1f2937;
  border-color: #fff;
}
.oauth--naver:hover { filter: brightness(0.95); }

.oauth--kakao{
  background:#FEE500; border-color:#F7D600; color:#191600;
}

.links {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  justify-content: center;
  font-size: 13px;
}
.links a {
  color: #2563eb;
  text-decoration: none;
}
.links a:hover {
  text-decoration: underline;
}


@media (max-height: 560px) {
  .auth-card {
    max-height: 90dvh;
    overflow: auto;
  }
}
</style>