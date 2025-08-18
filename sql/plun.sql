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
  `password` varchar(100) NOT NULL,
  `name` varchar(20) NOT NULL,
  `delete_yn` char(1) DEFAULT 'N',
  `delete_date` datetime DEFAULT NULL,
  `create_date` datetime NOT NULL DEFAULT current_timestamp(),
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`user_no`),
  KEY `TB_MEMBER_TB_ROLE_CODE_FK` (`role`),
  CONSTRAINT `TB_MEMBER_TB_ROLE_CODE_FK` FOREIGN KEY (`role`) REFERENCES `TB_ROLE_CODE` (`group_no`)
);

CREATE TABLE `TB_TEAM_MEMBER` (
  `team_no` int(11) NOT NULL,
  `user_no` int(11) NOT NULL,
  `role_no` varchar(4) NOT NULL,
  `join_date` datetime DEFAULT current_timestamp(),
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
('R001','대기중'),
('R002','녹음시작'),
('R003','녹음종료'),
('R004','파일 저장중'),
('R005','보관 만료')


INSERT INTO TB_MEMBER (role, email, password, name, delete_yn, delete_date, create_date, update_date) VALUES
('A001', 'admin1@example.com', 'pass1234', '관리자1', 'N', NULL, NOW(), NULL),
('A002', 'user1@example.com', 'pass1234', '회원1', 'N', NULL, NOW(), NULL),
('A002', 'user2@example.com', 'pass1234', '회원2', 'N', NULL, NOW(), NULL),
('A002', 'user3@example.com', 'pass1234', '회원3', 'N', NULL, NOW(), NULL)


회의 테이블 아직 생성X 지금은 초안만
-- 회의 테이블 생성은 추후에 진행할 예정입니다.
CREATE TABLE `tb_meeting_room` (
  `room_no` int(11) NOT NULL AUTO_INCREMENT,
  `team_no` int(11) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `scheduled_time` datetime NOT NULL DEFAULT current_timestamp(),
  `scheduled_end_time` datetime DEFAULT NULL,
  `started_time` datetime DEFAULT NULL,
  `ended_time` datetime DEFAULT NULL,
  `room_code` varchar(255) NOT NULL,
  `is_private` enum('Y','N') NOT NULL DEFAULT 'N',
  `room_password_hash` varchar(255) DEFAULT NULL,
  `create_date` datetime NOT NULL DEFAULT current_timestamp(),
  `update_date` datetime DEFAULT NULL ON UPDATE current_timestamp(),
  PRIMARY KEY (`room_no`),
  UNIQUE KEY `UK_MEETING_ROOM_CODE` (`room_code`),
  KEY `IDX_MEETING_SCHEDULED` (`scheduled_time`),
  KEY `IDX_MEETING_SCHEDULE_END` (`scheduled_end_time`),
  KEY `IDX_MEETING_TEAM_NO` (`team_no`),
  CONSTRAINT `FK_MEETING_TEAM_ROLE`
    FOREIGN KEY (`team_no`) REFERENCES `tb_team` (`team_no`) ON DELETE SET NULL,
  CONSTRAINT `CK_MEETING_SCHEDULE_RANGE`
    CHECK (`scheduled_end_time` IS NULL OR `scheduled_end_time` >= `scheduled_time`)
);

CREATE TABLE IF NOT EXISTS tb_meeting_recording (
  recording_no   INT NOT NULL AUTO_INCREMENT,
  room_no        INT NOT NULL,
  status_no      VARCHAR(4) NOT NULL,
  file_url       VARCHAR(500) DEFAULT NULL,
  duration_sec   INT DEFAULT NULL,
  started_time   DATETIME DEFAULT NULL,
  ended_time     DATETIME DEFAULT NULL,
  create_date    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_date    DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  expire_at      DATETIME NOT null,

  PRIMARY KEY (recording_no),
  UNIQUE KEY UK_MR_ROOM (room_no),
  KEY IDX_MR_STATUS (status_no),
  KEY IDX_MR_EXPIRE (expire_at),

  CONSTRAINT FK_MR_ROOM
    FOREIGN KEY (room_no) REFERENCES tb_meeting_room (room_no) ON DELETE CASCADE,
  CONSTRAINT FK_MR_STATUS
    FOREIGN KEY (status_no) REFERENCES tb_role_code (group_no)
);

CREATE TABLE IF NOT EXISTS tb_meeting_summary (
  summary_no   INT NOT NULL AUTO_INCREMENT,
  room_no      INT NOT NULL,  /* FK → tb_meeting_room */
  transcript   LONGTEXT DEFAULT NULL,  /* 전체 회의록 원본 */
  summary_text LONGTEXT DEFAULT NULL,  /* 요약본 */
  create_date  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_date  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (summary_no),
  KEY IDX_SUMMARY_ROOM (room_no),
  CONSTRAINT FK_SUMMARY_ROOM FOREIGN KEY (room_no)
    REFERENCES tb_meeting_room (room_no) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tb_meeting_participant (
  room_no       INT NOT NULL,           /* FK → tb_meeting_room */
  role_no       VARCHAR(4) NOT NULL,    /* FK → tb_role_code (참가자 역할) */
  user_no       INT NOT NULL,           /* FK → tb_member */
  join_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  out_time      DATETIME DEFAULT NULL,
  
  PRIMARY KEY (room_no, user_no),    
  KEY IDX_MP_ROLE (role_no),
  
  CONSTRAINT FK_MP_ROOM FOREIGN KEY (room_no)
    REFERENCES tb_meeting_room (room_no) ON DELETE CASCADE,
  CONSTRAINT FK_MP_ROLE FOREIGN KEY (role_no)
    REFERENCES tb_role_code (group_no),
  CONSTRAINT FK_MP_USER FOREIGN KEY (user_no)
    REFERENCES tb_member (user_no) ON DELETE CASCADE
);