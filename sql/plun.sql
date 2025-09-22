CREATE TABLE `TB_ROLE_CODE` (
  `group_no` varchar(4) NOT NULL,
  `group_name` varchar(20) NOT NULL,
  PRIMARY KEY (`group_no`)
);

CREATE TABLE `TB_TEAM` (
  `team_no` int(11) NOT NULL,
  `team_name` varchar(250) NOT NULL,
  `create_date` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`team_no`)
);

CREATE TABLE `TB_MEMBER` (
  `user_no` int(11) NOT NULL AUTO_INCREMENT,
  `role` varchar(4) NOT NULL,
  `email` varchar(40) NOT NULL,
  `password` varchar(100) DEFAULT NULL,
  `name` varchar(20) NOT NULL,
  `delete_yn` char(1) NOT NULL DEFAULT 'N',
  `delete_date` datetime DEFAULT NULL,
  `create_date` datetime NOT NULL DEFAULT current_timestamp(),
  `update_date` datetime DEFAULT NULL,
  `login_type_no` int(11) NOT NULL,
  PRIMARY KEY (`user_no`),
  KEY `TB_MEMBER_TB_ROLE_CODE_FK` (`role`),
  KEY `TB_MEMBER_TB_MEMBER_LOGIN_FK` (`login_type_no`),
  CONSTRAINT `TB_MEMBER_TB_MEMBER_LOGIN_FK` FOREIGN KEY (`login_type_no`) REFERENCES `TB_MEMBER_LOGIN` (`login_type_no`),
  CONSTRAINT `TB_MEMBER_TB_ROLE_CODE_FK` FOREIGN KEY (`role`) REFERENCES `TB_ROLE_CODE` (`group_no`)
)
CREATE TABLE `TB_MEMBER_LOGIN` (
  `login_type_no` int(11) NOT NULL AUTO_INCREMENT,
  `login_type_name` varchar(100) NOT NULL,
  PRIMARY KEY (`login_type_no`)
)

INSERT INTO `tb_member_login` (login_type_no, login_type_name)
VALUES 
(1, 'local'),
(2, 'social');

CREATE TABLE `TB_TEAM_MEMBER` (
  `team_no` int(11) NOT NULL,
  `user_no` int(11) NOT NULL,
  `role_no` varchar(4) NOT NULL,
  `join_date` datetime NOT NULL DEFAULT current_timestamp(),
  `delete_yn` char(1) NOT NULL DEFAULT 'N',
  KEY `TB_TEAM_MEMBER_TB_TEAM_FK` (`team_no`),
  KEY `TB_TEAM_MEMBER_TB_MEMBER_FK` (`user_no`),
  KEY `TB_TEAM_MEMBER_TB_ROLE_CODE_FK` (`role_no`),
  CONSTRAINT `TB_TEAM_MEMBER_TB_MEMBER_FK` FOREIGN KEY (`user_no`) REFERENCES `TB_MEMBER` (`user_no`),
  CONSTRAINT `TB_TEAM_MEMBER_TB_ROLE_CODE_FK` FOREIGN KEY (`role_no`) REFERENCES `TB_ROLE_CODE` (`group_no`),
  CONSTRAINT `TB_TEAM_MEMBER_TB_TEAM_FK` FOREIGN KEY (`team_no`) REFERENCES `TB_TEAM` (`team_no`)
);

INSERT INTO TB_ROLE_CODE (group_no, group_name) VALUES
('A001', '관리자'),
('A002', '회원'),
('B001', '팀장'),
('B002', '팀원'),
('C001', '방장'),
('C002', '참여자'),
('F001','대기'),
('F002','수락'),
('F003','거절')



INSERT INTO TB_MEMBER (role, email, password, name, delete_yn, delete_date, create_date, update_date) VALUES
('A001', 'admin1@example.com', 'pass1234', '관리자1', 'N', NULL, NOW(), NULL),
('A002', 'user1@example.com', 'pass1234', '회원1', 'N', NULL, NOW(), NULL),
('A002', 'user2@example.com', 'pass1234', '회원2', 'N', NULL, NOW(), NULL),
('A002', 'user3@example.com', 'pass1234', '회원3', 'N', NULL, NOW(), NULL)


CREATE TABLE `TB_INVITED` (
  `invited_id` int(11) NOT NULL AUTO_INCREMENT,
  `team_no` int(11) NOT NULL,
  `user_no` int(11) NOT NULL,
  `group_no` varchar(4) NOT NULL,
  `invited_email` varchar(40) NOT NULL,
  `create_date` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`invited_id`),
  KEY `TB_INVITED_TB_ROLE_CODE_FK` (`group_no`),
  KEY `TB_INVITED_TB_MEMBER_FK_1` (`user_no`),
  CONSTRAINT `TB_INVITED_TB_MEMBER_FK` FOREIGN KEY (`user_no`) REFERENCES `TB_MEMBER` (`user_no`),
  CONSTRAINT `TB_INVITED_TB_MEMBER_FK_1` FOREIGN KEY (`user_no`) REFERENCES `TB_MEMBER` (`user_no`),
  CONSTRAINT `TB_INVITED_TB_ROLE_CODE_FK` FOREIGN KEY (`group_no`) REFERENCES `TB_ROLE_CODE` (`group_no`)
);

CREATE TABLE `TB_CHAT_MEMBER` (
  `room_no` int(11) NOT NULL COMMENT '채팅방 번호 (FK)',
  `user_no` int(11) NOT NULL COMMENT '사용자 번호 (FK)',
  PRIMARY KEY (`room_no`,`user_no`),
  KEY `user_no` (`user_no`),
  CONSTRAINT `tb_chat_member_ibfk_1` FOREIGN KEY (`room_no`) REFERENCES `tb_chat_room` (`room_no`),
  CONSTRAINT `tb_chat_member_ibfk_2` FOREIGN KEY (`user_no`) REFERENCES `tb_member` (`user_no`)
);

CREATE TABLE `TB_CHAT_MESSAGE` (
  `message_no` int(11) NOT NULL AUTO_INCREMENT,
  `room_no` int(11) NOT NULL,
  `user_no` int(11) NOT NULL,
  `content` text NOT NULL,
  `create_date` datetime NOT NULL,
  `message_type` varchar(20) NOT NULL,
  PRIMARY KEY (`message_no`),
  KEY `room_no` (`room_no`),
  KEY `user_no` (`user_no`),
  CONSTRAINT `tb_chat_message_ibfk_1` FOREIGN KEY (`room_no`) REFERENCES `tb_chat_room` (`room_no`),
  CONSTRAINT `tb_chat_message_ibfk_2` FOREIGN KEY (`user_no`) REFERENCES `tb_member` (`user_no`)
);

CREATE TABLE `TB_CHAT_ROOM` (
  `room_no` int(11) NOT NULL AUTO_INCREMENT COMMENT '자동증가',
  `room_name` varchar(100) NOT NULL,
  `create_date` datetime NOT NULL DEFAULT current_timestamp() COMMENT '방 생성 시간',
  PRIMARY KEY (`room_no`)
);
CREATE TABLE `TB_ATTACHMENT` (
  `attachment_no` varchar(36) NOT NULL COMMENT 'UUID 형식',
  `message_no` int(11) NOT NULL COMMENT '메시지 번호 (FK)',
  `original_name` varchar(100) NOT NULL,
  `file_name` varchar(100) NOT NULL,
  `path` varchar(300) NOT NULL COMMENT '저장 경로',
  `size` int(11) NOT NULL COMMENT '파일 크기 (바이트)',
  `content_type` varchar(100) NOT NULL,
  `extension` varchar(10) NOT NULL DEFAULT 'PDF,JPG',
  `create_date` datetime NOT NULL DEFAULT current_timestamp(),
  `delete_date` datetime DEFAULT NULL,
  PRIMARY KEY (`attachment_no`),
  KEY `message_no` (`message_no`),
  CONSTRAINT `tb_attachment_ibfk_1` FOREIGN KEY (`message_no`) REFERENCES `tb_chat_message` (`message_no`)
);

CREATE TABLE TB_ALARM (
  alarm_no int(11) NOT NULL AUTO_INCREMENT COMMENT '자동증가',
  user_no int(11) NOT NULL COMMENT '사용자 번호 (FK)',
  alarm_type varchar(20) NOT NULL COMMENT 'CHAT / CALENDAR 등',
  reference_no int(11) NOT NULL COMMENT '연관된 엔티티 번호 (예: message_no, cal_no)',
  content text NOT NULL,
  is_read char(1) NOT NULL DEFAULT 'N' COMMENT '(Y/N 구분)',
  create_date datetime NOT NULL DEFAULT current_timestamp(),
  sender_no int(11) DEFAULT NULL,
  PRIMARY KEY (alarm_no),
  KEY user_no (user_no),
  CONSTRAINT tb_alarm_ibfk_1 FOREIGN KEY (user_no) REFERENCES tb_member (user_no)
);


CREATE TABLE `TB_MEETING_PARTICIPANT` (
  `room_no` int(11) NOT NULL,
  `role_no` varchar(4) NOT NULL,
  `user_no` int(11) NOT NULL,
  `join_time` datetime DEFAULT NULL,
  `out_time` datetime DEFAULT NULL,
  PRIMARY KEY (`room_no`,`user_no`),
  KEY `IDX_MP_ROLE` (`role_no`),
  KEY `FK_MP_USER` (`user_no`),
  CONSTRAINT `FK_MP_ROLE` FOREIGN KEY (`role_no`) REFERENCES `tb_role_code` (`group_no`),
  CONSTRAINT `FK_MP_ROOM` FOREIGN KEY (`room_no`) REFERENCES `tb_meeting_room` (`room_no`) ON DELETE CASCADE,
  CONSTRAINT `FK_MP_USER` FOREIGN KEY (`user_no`) REFERENCES `tb_member` (`user_no`) ON DELETE CASCADE
)

CREATE TABLE `TB_MEETING_ROOM` (
  `room_no` int(11) NOT NULL AUTO_INCREMENT,
  `team_no` int(11) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `scheduled_time` datetime NOT NULL DEFAULT current_timestamp(),
  `scheduled_end_time` datetime DEFAULT NULL,
  `started_time` datetime DEFAULT NULL,
  `ended_time` datetime DEFAULT NULL,
  `room_code` varchar(255) NOT NULL,
  `create_date` datetime NOT NULL DEFAULT current_timestamp(),
  `update_date` datetime DEFAULT NULL ON UPDATE current_timestamp(),
  `cal_detail_no` int(11) DEFAULT NULL COMMENT '생성된 달력상세 PK',
  PRIMARY KEY (`room_no`),
  UNIQUE KEY `UK_MEETING_ROOM_CODE` (`room_code`),
  KEY `IDX_MEETING_SCHEDULED` (`scheduled_time`),
  KEY `IDX_MEETING_SCHEDULE_END` (`scheduled_end_time`),
  KEY `IDX_MEETING_TEAM_NO` (`team_no`),
  KEY `idx_mr_cal_detail` (`cal_detail_no`),
  CONSTRAINT `FK_MEETING_TEAM_ROLE` FOREIGN KEY (`team_no`) REFERENCES `tb_team` (`team_no`) ON DELETE SET NULL,
  CONSTRAINT `CK_MEETING_SCHEDULE_RANGE` CHECK (`scheduled_end_time` is null or `scheduled_end_time` >= `scheduled_time`)
)

CREATE TABLE `TB_MEETING_SUMMARY` (
  `room_no` int(11) NOT NULL,
  `summary` longtext DEFAULT NULL,
  `action_items` longtext DEFAULT NULL,
  `decisions` longtext DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`room_no`),
  CONSTRAINT `fk_ms_room` FOREIGN KEY (`room_no`) REFERENCES `tb_meeting_room` (`room_no`) ON DELETE CASCADE ON UPDATE CASCADE
)

CREATE TABLE `TB_MEETING_TRANSCRIPT` (
  `room_no` int(11) NOT NULL,
  `merged_text` longtext DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`room_no`),
  CONSTRAINT `fk_mt_room` FOREIGN KEY (`room_no`) REFERENCES `tb_meeting_room` (`room_no`) ON DELETE CASCADE ON UPDATE CASCADE

CREATE TABLE TB_CALENDAR (
  cal_no int(11) NOT NULL AUTO_INCREMENT COMMENT '자동증가',
  team_no int(11) NOT NULL COMMENT '팀 번호 (FK)',
  user_no int(11) NOT NULL COMMENT '작성자 (FK)',
  PRIMARY KEY (cal_no),
  KEY team_no (team_no),
  KEY user_no (user_no),
  CONSTRAINT tb_calendar_ibfk_1 FOREIGN KEY (team_no) REFERENCES tb_team (team_no),
  CONSTRAINT tb_calendar_ibfk_2 FOREIGN KEY (user_no) REFERENCES tb_member (user_no)
)

CREATE TABLE TB_CALENDAR_DETAIL (
  cal_detail_no int(11) NOT NULL AUTO_INCREMENT COMMENT '자동증가',
  cal_no int(11) NOT NULL COMMENT '달력 번호 (FK)',
  contents text DEFAULT NULL,
  start_date date NOT NULL COMMENT '시작 날짜 (YYYY-MM-DD)',
  start_time time DEFAULT NULL COMMENT '시작 시간 (HH:mm:ss)',
  end_date date NOT NULL COMMENT '종료 날짜 (YYYY-MM-DD)',
  end_time time DEFAULT NULL COMMENT '종료 시간 (HH:mm:ss)',
  delete_yn char(1) NOT NULL DEFAULT 'N',
  create_date datetime NOT NULL DEFAULT current_timestamp(),
  reg_user_no int(11) NOT NULL COMMENT '등록자 (FK)',
  update_dt datetime DEFAULT NULL,
  update_user_no int(11) DEFAULT NULL,
  title varchar(100) NOT NULL,
  PRIMARY KEY (cal_detail_no),
  KEY cal_no (cal_no),
  KEY reg_user_no (reg_user_no),
  KEY update_user_no (update_user_no),
  CONSTRAINT tb_calendar_detail_ibfk_1 FOREIGN KEY (cal_no) REFERENCES tb_calendar (cal_no),
  CONSTRAINT tb_calendar_detail_ibfk_2 FOREIGN KEY (reg_user_no) REFERENCES tb_member (user_no),
  CONSTRAINT tb_calendar_detail_ibfk_3 FOREIGN KEY (update_user_no) REFERENCES tb_member (user_no)
)

CREATE TABLE TB_CALENDAR_DETAIL_PARTICIPANT (
  cal_detail_no int(11) NOT NULL COMMENT 'FK → tb_calendar_detail.cal_detail_no',
  user_no int(11) NOT NULL COMMENT 'FK → tb_member.user_no',
  PRIMARY KEY (cal_detail_no,user_no),
  KEY idx_cdp_user_no (user_no),
  CONSTRAINT fk_cdp_cal_detail FOREIGN KEY (cal_detail_no) REFERENCES tb_calendar_detail (cal_detail_no) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_cdp_user FOREIGN KEY (user_no) REFERENCES tb_member (user_no) ON UPDATE CASCADE
)

CREATE TABLE `TB_MEMBER_LOGIN` (
  `login_type_no` int(11) NOT NULL AUTO_INCREMENT,
  `login_type_name` varchar(100) NOT NULL,
  PRIMARY KEY (`login_type_no`)
)

INSERT INTO TB_MEMBER_LOGIN (login_type_name) VALUES
('local'),
('social')