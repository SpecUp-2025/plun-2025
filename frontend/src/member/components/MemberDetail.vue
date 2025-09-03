<template>
    <section class="card">
      <dl class="list">
        <div class="row">
          <dt>이름</dt>
          <dd>{{ name }}</dd>
        </div>
        <div class="row">
          <dt>이메일</dt>
          <dd>{{ email }}</dd>
        </div>
      </dl>
  
      <details ref="pwdDetail">
        <summary class="s">
          <span>비밀번호 변경</span>
          <span>최근 변경 이력 : {{ form.date.updateDate }}</span>
        </summary>
  
        <input v-model="form.currentPassword" type="password" placeholder="현재 비밀번호" />
        <input v-model="form.password" type="password" placeholder="새 비밀번호" @input="checkPassword" />
        <div v-show="unvaild.password" v-text="unvaild.password"></div>
  
        <input v-model="passwordCheck" type="password" placeholder="비밀번호 확인" @input="checkPasswordCheck" />
        <div v-show="unvaild.passwordCheck" v-text="unvaild.passwordCheck"></div>

        <button @click="sendPassword">확인</button>
        <button type="button" @click="onCancelPwd">취소</button>
      </details>
  
      <details ref="delDetail">
        <summary><span>계정 탈퇴</span></summary>
        <span>계정 탈퇴시 기존 데이터는 삭제 됩니다.</span>
        <div class="panel"><button class="danger-btn" @click="userDelete">계정 삭제</button></div>
        <div class="panel"><button class="danger-btn" type="button" @click="onCancelDelete">취소</button></div>
      </details>
    </section>
    <button @click="router.back()">뒤로가기</button>
  </template>

<script setup>
import { useUserStore } from '@/store/userStore';
import instance from '@/util/interceptors';
import { computed, onMounted, reactive, ref } from 'vue';
import { REGEX_PATTERN } from '../util/Regex';
import { useRouter } from 'vue-router';

const router = useRouter();
const useStore = useUserStore();
const email = computed(() => useStore.user.email ?? '');
const name = computed(() => useStore.user.name ?? '');
const userNo = computed(() => useStore.user.userNo ?? '');

const form = reactive({
  date: {},
  password: '',
  currentPassword: '',
});
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
}


const userDelete = async () =>{
    try {
        const {status,data} = await instance.put(`member/userDelete/${userNo.value}`)
        if(status ===200 && data.success){
            alert("탈되 되셨습니다.")
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
    console.error('불러오기 실패 ', error);
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

<style lang="scss" scoped>

</style>