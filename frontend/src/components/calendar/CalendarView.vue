<style src="@/styles/components/calendar.css"></style>
<template>
  <div>

    <!-- 알림 메시지 표시 UI 추가 -->
    <div class="notification-area" v-if="notifications.length">
      <div
        v-for="(n, index) in notifications"
        :key="index"
        :class="['notification', n.type]"
      >
        {{ n.message }}
      </div>
    </div>

    <FullCalendar ref="fullCalendar" :options="calendarOptions" />

    <CalendarRegModal
      :showModal="showModal"
      :formData="formData"
      :teamMembers="teamMembers"
      @close="showModal = false"
      @save="saveEvent"
      @delete="deleteEvent"
    />
  </div>
</template>

<script>
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import instance from '@/util/interceptors';
import { useUserStore } from '@/store/userStore';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import CalendarRegModal from './CalendarRegModal.vue';
import { useAlarmStore } from '@/store/useAlarmStore';

export default {
  components: { FullCalendar, CalendarRegModal },
  data() {
    return {
      notifications: [],
      stompClient: null,
      isConnected: false,
      userStore: useUserStore(),
      alarmStore: useAlarmStore(),
      teamNo: this.$route.params.teamNo,
      teamMembers: [],
      calendarNo: null,
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
      eventContent: function (arg) {
        const timeText = arg.timeText;
        const title = arg.event.title;

        return {
          html: `
            <div class="custom-event">
              <div class="event-time">${timeText}</div>
              <div class="event-title">${title}</div>
            </div>
          `,
        };
      },
        dateClick: this.handleDateClick,
        eventClick: this.handleEventClick,
        eventDrop: this.handleEventDrop,
        datesSet: this.handleDatesSet,
        height: 800,
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
        participantUserNos: []
      },
    };
  },
  methods: {

    sendWebSocketMessage(message) {
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.send('/app/calendar/refresh', {}, message);
        console.log('📤 WebSocket 메시지 전송됨:', message);
      } else {
        console.warn('⚠️ WebSocket이 연결되어 있지 않음');
      }
    },
    connectWebSocket() {
        if (this.stompClient && this.isConnected) {
          console.log('⚠️ 이미 WebSocket 연결됨 - 중복 방지');
          return;
        }

        const socket = new SockJS('/ws-chat');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, () => {
          const userNo = this.userStore.user?.userNo;

          if (userNo) {
            this.stompClient.subscribe(`/topic/calendar/refresh/${userNo}`, (message) => {
              const body = message.body;
              // ✅ 자기 자신이 보낸 메시지는 무시
              if (body.senderUserNo === this.userStore.user?.userNo) {
                console.log('🔁 자기 자신이 보낸 메시지, 무시함');
                return;
              }
              console.log('📨 [WebSocket] 메시지 수신:', body);
              this.handleNotificationMessage(body);
            });
              this.stompClient.subscribe(`/topic/notifications/${userNo}`, (message) => {
                  console.log('🔔 [WebSocket] 알림 수신:', message.body);
                  const alarm = JSON.parse(message.body);
                  
                  
                  this.alarmStore.addAlarm(alarm);
                  console.log('✅ 알림이 alarmStore에 추가됨:', alarm);
              });
            this.isConnected = true;
          }
        }, (error) => {
          console.error('WebSocket 연결 실패:', error);
        });
    },
    handleNotificationMessage(body) {
      let message = '';
      let type = '';

      if (body.startsWith('eventDeleted:')) {
        message = '🗑️ 일정이 삭제되었습니다.';
        type = 'delete';
      } else if (body.startsWith('eventCreated')) {
        message = '🔔 새로운 일정이 등록되었습니다.';
        type = 'new';
      } else if (body.startsWith('eventUpdated')) {
        message = '✏️ 일정이 수정되었습니다.';
        type = 'update';
      }

      if (message && !this.notifications.some(n => n.message === message)) {
        const notification = { 
          id: Date.now() + Math.random(),
          type, 
          message 
        };
        this.notifications.push(notification);

        setTimeout(() => {
          const index = this.notifications.findIndex(n => n.id === notification.id);
          if (index > -1) {
            this.notifications.splice(index, 1);
          }
        }, 3000);
      }

      this.fetchUserEvents();
    },
    handleEventDeleted(calDetailNo) {
      console.log('🔧 handleEventDeleted 호출됨, 삭제할 ID:', calDetailNo);

      console.log('🔎 현재 calendarEvents:', this.calendarEvents);

      const beforeLength = this.calendarEvents.length;
      this.calendarEvents = this.calendarEvents.filter(
        (event) => String(event.id) !== String(calDetailNo)
      );
      const afterLength = this.calendarEvents.length;

      console.log(`🧹 삭제 전 이벤트 수: ${beforeLength}, 삭제 후: ${afterLength}`);

      this.$nextTick(() => {
        this.$refs.fullCalendar?.getApi().refetchEvents();
      });
    },

    async checkOrCreateCalendar() {
      const teamNo = this.$route.params.teamNo;
      const userNo = this.userStore.user?.userNo;

      try {
        let { data: existingCalNo } = await instance.get('/calendar/calno', {
          params: { teamNo, userNo }
        });

        if (!existingCalNo) {
          await instance.post('/calendar/create', { teamNo, userNo });

          const { data: newCalNo } = await instance.get('/calendar/calno', {
            params: { teamNo, userNo }
          });
          this.calendarNo = newCalNo;
          console.log(' 캘린더 자동 생성 완료, calendarNo:', this.calendarNo);
        } else {
          this.calendarNo = existingCalNo;
          console.log(' 이미 캘린더 존재함:', this.calendarNo);
        }
      } catch (error) {
        console.error(' 캘린더 확인/생성 중 오류 발생:', error);
      }
    },

    async fetchTeamMembers() {
      const teamNo = this.$route.params.teamNo;
      const userNo = this.userStore.user?.userNo;

      try {
        const { data } = await instance.get(`/teams/${teamNo}/members`);
        this.teamMembers = data.map(member => ({
          ...member,
          userNo: Number(member.userNo),
          isSelf: member.userNo === userNo
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

        this.sendWebSocketMessage(`eventDeleted:${this.formData.calDetailNo}`);

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
          return time.split('+')[0];
        }
        if (time.length === 5) return `${time}:00`;
        if (time.length === 8) return time;
        return '00:00:00';  
      },

      toISODate(dateStr) {
        if (!dateStr) return '';
        if (dateStr.includes('-')) return dateStr;
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
            participantUserNos: item.participantUserNos || [],
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
          const userNo = this.userStore.user?.userNo;

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
        console.log('calendarEvents 업데이트됨:', this.calendarEvents);
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
        participantUserNos: [this.userStore.user?.userNo]
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
        regUserNo: props.regUserNo,
        title: info.event.title,
        contents: props.contents,
        startDate: start.split('T')[0],
        startTime: this.normalizeTime(start.split('T')[1] || '00:00:00'),
        endDate: end ? end.split('T')[0] : start.split('T')[0],
        endTime: this.normalizeTime(end.split('T')[1] || '23:59'),
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
  try {
    console.log('saveEvent 호출됨', new Date().toISOString());
    console.log('saveEvent 호출 - calDetailNo:', this.formData.calDetailNo);

    const formatTime = (timeStr) => {
      if (!timeStr || !timeStr.includes(':')) return '00:00:00';
      if (timeStr.length === 5) return `${timeStr}:00`;
      if (timeStr.includes('+')) return timeStr.split('+')[0];
      return timeStr;
    };

    const participantSet = new Set(this.formData.participantUserNos || []);
    const creatorNo = this.formData.regUserNo;
    const myUserNo = this.userStore.user?.userNo;

    if (creatorNo) participantSet.add(Number(creatorNo));
    if (myUserNo) participantSet.add(Number(myUserNo));

    const payload = {
      detail: {
        ...this.formData,
        calNo: this.calendarNo,
        regUserNo: this.userStore.user?.userNo,
        startTime: formatTime(this.formData.startTime),
        endTime: formatTime(this.formData.endTime),
      },
      participantUserNos: [...participantSet],
    };

    console.log('팀원 리스트:', this.formData.participantUserNos);
    console.log('저장할 payload:', payload);

    const isUpdate = !!payload.detail.calDetailNo;

    let referenceNo = null;

    if (isUpdate) {
      await instance.put('/calendar/event', payload);
      this.sendWebSocketMessage('eventUpdated');
      console.log('PUT 응답:', payload);
      referenceNo = this.formData.calDetailNo;
    } else {
      await instance.post('/calendar/event', payload);
      this.sendWebSocketMessage('eventCreated');
      console.log('POST 응답:', payload);
    }

    const alarmType = isUpdate ? 'CALENDAR_UPDATE' : 'CALENDAR_CREATE';
    const title = this.formData.title;
    const teamNo = this.$route.params.teamNo;
    const senderName = this.userStore.user?.name || '시스템';
    const participants = [...participantSet];

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

          const regUserNo = info.event.extendedProps?.regUserNo;
          const currentUserNo = this.userStore.user?.userNo;

          if (Number(regUserNo) !== Number(currentUserNo)) {
            alert('⚠️ 다른 사용자가 등록한 일정은 이동할 수 없습니다.');
            info.revert();
            return;
          }

        const detailPayload = {
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

        const participantUserNos = info.event.extendedProps.participantUserNos || [];

        try {
          await instance.put('/calendar/event', {
            detail: detailPayload,
            participantUserNos: participantUserNos,
          });
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
        this.connectWebSocket();
      },
    };
</script>