<template>
  <div>
    <FullCalendar ref="fullCalendar" :options="calendarOptions" />

    <!-- 일정 등록 모달 -->
    <div v-if="showModal" class="modal">
      <h3>일정 등록</h3>

      <!-- 팀원 초대 -->
      <label>팀원 초대</label>

      <button @click="showTeamMemberSelector = !showTeamMemberSelector">
        팀원 선택 ({{ formData.participantUserNos.length }}명)
      </button>

      <!-- 팀원 목록은 필요할 때만 보여줌 -->
      <div v-if="showTeamMemberSelector" style="margin-top: 8px;">
        <div v-for="member in teamMembers" :key="member.userNo">
          <input
            type="checkbox"
            :value="Number(member.userNo)"
            v-model="formData.participantUserNos"
          />
          {{ member.name }}
        </div>
      </div>

      <label>제목</label>
      <input v-model="formData.title" type="text" />

      <label>내용</label>
      <textarea v-model="formData.contents"></textarea>

      <label>시작 시간</label>
      <input v-model="formData.startTime" type="time" />

      <label>종료 시간</label>
      <input v-model="formData.endTime" type="time" />

      <br />
      <button @click="saveEvent">저장</button>
      <button @click="showModal = false">취소</button>
      <button v-if="formData.calDetailNo" @click="deleteEvent">삭제</button>
    </div>
  </div>
</template>

<script>
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import instance from '@/util/interceptors';
import { useUserStore } from '@/store/userStore';

export default {
  components: { FullCalendar },
  data() {
    return {
      userStore: useUserStore(),
      teamNo: this.$route.params.teamNo,
      teamMembers: [],
      calendarNo: null, // 캘린더 번호 저장
      calendarEvents: [],
      showModal: false,
      showTeamMemberSelector: false,
      calendarOptions: {
        plugins: [dayGridPlugin, interactionPlugin, timeGridPlugin],
        initialView: 'dayGridMonth',
        locale: 'ko',
        headerToolbar: {
          left: 'prev,next today',
          center: 'title',
          right: 'dayGridMonth,timeGridWeek,timeGridDay',
        },
        views: {
        dayGridMonth: { buttonText: '월별' },
        timeGridWeek: { buttonText: '주별' },
        timeGridDay: { buttonText: '일별'} },
        editable: true,
        navLinks: true,
        displayEventTime: true,
        eventTimeFormat: {
          hour: '2-digit',
          minute: '2-digit',
          hour12: true,
        },
        dateClick: this.handleDateClick,
        eventClick: this.handleEventClick,
        eventDrop: this.handleEventDrop,
        datesSet: this.handleDatesSet,
        // ✅ 올바른 콜백 함수 형식
        events: (fetchInfo, successCallback, failureCallback) => {
        successCallback(this.calendarEvents);
      },
      },
      formData: {
        calDetailNo: null,
        calNo: null,
        regUserNo: null,
        title: '',
        contents: '',
        startDate: '',
        startTime: '',
        endDate: '',
        endTime: '',
        teamMembers: [],
        participantUserNos: []  // 초대된 팀원 user_no 리스트
      },
    };
  },
  methods: {

    // ✅ 캘린더 존재하지 않으면 생성
    async checkOrCreateCalendar() {
      const teamNo = this.$route.params.teamNo;
      const userNo = this.userStore.user?.userNo;

      try {
        let { data: existingCalNo } = await instance.get('/calendar/calno', {
          params: { teamNo, userNo }
        });

        if (!existingCalNo) {
          await instance.post('/calendar/create', { teamNo, userNo });
          // 캘린더 생성 후 다시 조회
          const { data: newCalNo } = await instance.get('/calendar/calno', {
            params: { teamNo, userNo }
          });
          this.calendarNo = newCalNo;
          console.log('📅 캘린더 자동 생성 완료, calendarNo:', this.calendarNo);
        } else {
          this.calendarNo = existingCalNo;
          console.log('✅ 이미 캘린더 존재함:', this.calendarNo);
        }
      } catch (error) {
        console.error('⚠️ 캘린더 확인/생성 중 오류 발생:', error);
      }
    },

    async fetchTeamMembers() {
      const teamNo = this.$route.params.teamNo;
      const userNo = this.userStore.user?.userNo;

      try {
        const { data } = await instance.get(`/teams/${teamNo}/members`);
        // 본인(userNo)을 제외한 팀원 목록만 저장
          this.teamMembers = data
            .filter(member => member.userNo !== userNo)
            .map(member => ({
              ...member,
              userNo: Number(member.userNo)
            }));
            
          console.log('📋 팀원 목록:', this.teamMembers);
        } catch (error) {
        console.error('❌ 팀원 목록 조회 실패:', error);
      }
    },

    async deleteEvent() {
      if (!this.formData.calDetailNo) return;

      const confirmed = confirm('정말로 이 일정을 삭제하시겠습니까?');
      if (!confirmed) return;

      try {
        await instance.delete('/calendar/event', {
          params: { calDetailNo: this.formData.calDetailNo },
        });

        alert('일정이 삭제되었습니다.');
        this.showModal = false;
        this.fetchUserEvents();
      } catch (error) {
        console.error('일정 삭제 실패:', error);
        alert('삭제에 실패했습니다.');
      }
    },

    normalizeDate(yyyymmdd) {
      if (!yyyymmdd) return '';
      if (yyyymmdd.includes('-')) return yyyymmdd;
      return `${yyyymmdd.slice(0, 4)}-${yyyymmdd.slice(4, 6)}-${yyyymmdd.slice(6, 8)}`;
    },

    normalizeTime(time) {
        if (!time) return '00:00:00';
        if (time.includes('+')) {
          return time.split('+')[0]; // 타임존 제거
        }
        if (time.length === 5) return `${time}:00`; // 'HH:mm'
        if (time.length === 8) return time;  // 'HH:mm:ss' → 'HH:mm'
        return '00:00:00';  
      },

      toISODate(dateStr) {
        if (!dateStr) return '';
        if (dateStr.includes('-')) return dateStr; // 이미 ISO 날짜 형식이면 그대로 리턴
        if (dateStr.length === 8) {
          return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
        }
        return '';
      },

      async handleDatesSet(info) {
        const formatDate = (date) => date.toISOString().slice(0, 10);
        const start = formatDate(info.start);
        const end = formatDate(info.end);
        const userNo = this.userStore.user?.userNo;
        console.log('userNo:', userNo);
      try {
        const { data } = await instance.get('/calendar/events', {
          params: { start, end, userNo  },
        });

        const events = data.map((item) => ({
          id: item.calDetailNo,
          title: item.title,
          start: `${this.toISODate(item.startDate)}T${item.startTime || '00:00:00'}`,
          end: `${this.toISODate(item.endDate)}T${item.endTime || '23:59:59'}`,
          extendedProps: {
            ...item,
            participantUserNos: item.participantUserNos || [],  // 서버에서 받아온 멤버 리스트 필드 이름에 맞게 수정
          },
        }));

        this.calendarEvents = events;
        this.$nextTick(() => {
          this.$refs.fullCalendar?.getApi().refetchEvents();
        });

        this.$nextTick(() => {
          this.$refs.fullCalendar?.getApi().refetchEvents();
        });
      } catch (error) {
        console.error('📛 이벤트 불러오기 실패:', error);
      }
    },

      async fetchUserEvents() {
        try {
          const userNo = this.userStore.user?.userNo;  // 사용자 번호 얻기

          const { data } = await instance.get('/calendar/events', {
            params: {
              start: '2025-01-01',
              end: '2025-12-31',
              userNo,
            },
          });
        console.log('서버에서 받아온 이벤트 데이터:', data);

        const events = data.map((item) => ({
          id: item.calDetailNo,
          title: item.title,
          start: `${this.toISODate(item.startDate)}T${item.startTime || '00:00:00'}`,
          end: `${this.toISODate(item.endDate)}T${item.endTime || '23:59:59'}`,
          extendedProps: {
            ...item,
            participantUserNos: item.participantUserNos || [],
          },
        }));

        console.log('변환된 이벤트 배열:', events);
        this.calendarEvents = events;
        this.$nextTick(() => {
          this.$refs.fullCalendar?.getApi().refetchEvents();
        });
      } catch (error) {
        console.error('일정 목록 조회 실패:', error);
      }
    },

    handleDateClick(info) {
      const selectedDate = info.dateStr || new Date().toISOString().split('T')[0];
      this.formData = {
        calDetailNo: null,
        calNo: this.calendarNo,
        regUserNo: this.userStore.user?.userNo,
        title: '',
        contents: '',
        startDate: selectedDate,
        startTime: '00:00',
        endDate: selectedDate,
        endTime: '00:00',
        participantUserNos: []  // 초대된 팀원 user_no 리스트
        
      };
      this.showModal = true;
    },

    handleEventClick(info) {

      const props = info.event.extendedProps;
      console.log('🔍 클릭한 이벤트의 extendedProps:', props);
      console.log('🔍 props.participantUserNos:', props.participantUserNos);
      const start = info.event.startStr;
      const end = info.event.endStr || '';
      this.formData = {
        calDetailNo: info.event.id,
        calNo: this.calendarNo,
        regUserNo: this.userStore.user?.userNo,
        title: info.event.title,
        contents: props.contents,
        startDate: start.split('T')[0],
        startTime: this.normalizeTime(start.split('T')[1] || '00:00:00'),
        endDate: end ? end.split('T')[0] : start.split('T')[0],
        endTime: this.normalizeTime(end.split('T')[1] || '23:59:59'),
        participantUserNos: (props.participantUserNos || []).map(Number),
      };
      this.showModal = true;
    },

    async saveEvent() {
      if (this.isSaving) {
      console.log('저장 중복 호출 방지');
      return;
      }
    this.isSaving = true;
    try{
      console.log('saveEvent 호출됨', new Date().toISOString());
      console.log('saveEvent 호출 - calDetailNo:', this.formData.calDetailNo);
      const formatTime = (timeStr) => {
        if (!timeStr || !timeStr.includes(':')) return '00:00:00';
        if (timeStr.length === 5) return `${timeStr}:00`;  // HH:mm → HH:mm:00
        if (timeStr.includes('+')) return timeStr.split('+')[0]; // ✅ 타임존 제거
        return timeStr;
      };
      const payload = {
              detail: {
                ...this.formData,
                calNo: this.calendarNo,
                regUserNo: this.userStore.user?.userNo,
                startTime: formatTime(this.formData.startTime),
                endTime: formatTime(this.formData.endTime),
              },
              participantUserNos: this.formData.participantUserNos,
            };
            console.log('팀원 리스트:', this.formData.participantUserNos);

          if (payload.detail.calDetailNo) {
            await instance.put('/calendar/event', payload);
          } else {
            await instance.post('/calendar/event', payload);
          }
          this.showModal = false;
          await this.fetchUserEvents();

        } catch (error) {
          console.error('일정 저장 실패:', error);
        } finally {
          this.isSaving = false;
        }
      },

      async handleEventDrop(info) {
        const getTime = (datetimeStr, defaultTime) => {
          if (!datetimeStr) return defaultTime;
          const timePart = datetimeStr.split('T')[1];
          return this.normalizeTime(timePart?.split('+')[0] || defaultTime);
        };

        const payload = {
          calDetailNo: info.event.id,
          title: info.event.title,
          calNo: this.calendarNo,
          regUserNo: this.userStore.user?.userNo || null,
          contents: info.event.extendedProps.contents,
          startDate: info.event.startStr.split('T')[0],
          startTime: getTime(info.event.startStr, '00:00:00'),
          endDate: info.event.endStr ? info.event.endStr.split('T')[0] : info.event.startStr.split('T')[0],
          endTime: info.event.endStr ? getTime(info.event.endStr, '23:59:59') : '',
        };

      try {
            await instance.put('/calendar/event', payload);
            this.fetchUserEvents();
          } catch (error) {
            console.error('이벤트 드래그 저장 실패:', error);
            info.revert();
          }
        },
      },
      mounted() {
        this.checkOrCreateCalendar();
        this.fetchUserEvents();
        this.fetchTeamMembers();
      },
    };
</script>

<style scoped>
.modal {
  position: fixed;
  top: 20%;
  left: 30%;
  width: 300px;
  background: white;
  padding: 20px;
  border: 1px solid #ccc;
  z-index: 999;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}
.member-selector {
  border: 1px solid #ccc;
  padding: 8px;
  margin-top: 8px;
  max-height: 150px;
  overflow-y: auto;
}
</style>