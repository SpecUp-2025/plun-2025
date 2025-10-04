CREATE TABLE IF NOT EXISTS TB_ROLE_CODE (
  group_no   VARCHAR(4)  NOT NULL,
  group_name VARCHAR(20) NOT NULL,
  PRIMARY KEY (group_no)
);

CREATE TABLE IF NOT EXISTS TB_MEMBER_LOGIN (
  login_type_no   INT NOT NULL AUTO_INCREMENT,
  login_type_name VARCHAR(100) NOT NULL,
  PRIMARY KEY (login_type_no)
);

CREATE TABLE IF NOT EXISTS TB_TEAM (
  team_no    INT NOT NULL AUTO_INCREMENT,
  team_name  VARCHAR(250) NOT NULL,
  create_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (team_no)
);

CREATE TABLE IF NOT EXISTS TB_MEMBER (
  user_no       INT NOT NULL AUTO_INCREMENT,
  role          VARCHAR(4)  NOT NULL,
  email         VARCHAR(40) NOT NULL,
  password      VARCHAR(100) DEFAULT NULL,
  name          VARCHAR(20) NOT NULL,
  delete_yn     CHAR(1)     NOT NULL DEFAULT 'N',
  create_date   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_date   DATETIME    DEFAULT NULL,
  login_type_no INT         NOT NULL,
  PRIMARY KEY (user_no),
  KEY idx_member_role (role),
  KEY idx_member_login (login_type_no),
  CONSTRAINT fk_member_role  FOREIGN KEY (role)          REFERENCES TB_ROLE_CODE (group_no),
  CONSTRAINT fk_member_login FOREIGN KEY (login_type_no) REFERENCES TB_MEMBER_LOGIN (login_type_no)
);

CREATE TABLE IF NOT EXISTS TB_TEAM_MEMBER (
  team_no  INT NOT NULL,
  user_no  INT NOT NULL,
  role_no  VARCHAR(4) NOT NULL,
  join_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delete_yn CHAR(1)   NOT NULL DEFAULT 'N',
  KEY fk_tm_team   (team_no),
  KEY fk_tm_user   (user_no),
  KEY fk_tm_role   (role_no),
  CONSTRAINT fk_tm_team FOREIGN KEY (team_no) REFERENCES TB_TEAM (team_no),
  CONSTRAINT fk_tm_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no),
  CONSTRAINT fk_tm_role FOREIGN KEY (role_no) REFERENCES TB_ROLE_CODE (group_no)
);

CREATE TABLE IF NOT EXISTS TB_INVITED (
  invited_id   INT NOT NULL AUTO_INCREMENT,
  team_no      INT NOT NULL,
  user_no      INT NOT NULL,
  group_no     VARCHAR(4) NOT NULL,
  invited_email VARCHAR(40) NOT NULL,
  create_date  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (invited_id),
  KEY fk_inv_role  (group_no),
  KEY fk_inv_user  (user_no),
  CONSTRAINT fk_inv_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no),
  CONSTRAINT fk_inv_role FOREIGN KEY (group_no) REFERENCES TB_ROLE_CODE (group_no)
);

CREATE TABLE IF NOT EXISTS TB_CHAT_ROOM (
  room_no    INT NOT NULL AUTO_INCREMENT COMMENT '자동증가',
  room_name  VARCHAR(100) NOT NULL,
  create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '방 생성 시간',
  team_no INT NOT NULL COMMENT '채팅방 생성 팀 번호',
  PRIMARY KEY (room_no)
);

CREATE TABLE IF NOT EXISTS TB_CHAT_MEMBER (
  room_no INT NOT NULL COMMENT '채팅방 번호 (FK)',
  user_no INT NOT NULL COMMENT '사용자 번호 (FK)',
  PRIMARY KEY (room_no, user_no),
  KEY fk_cm_user (user_no),
  CONSTRAINT fk_cm_room FOREIGN KEY (room_no) REFERENCES TB_CHAT_ROOM (room_no),
  CONSTRAINT fk_cm_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no)
);

CREATE TABLE IF NOT EXISTS TB_CHAT_MESSAGE (
  message_no INT NOT NULL AUTO_INCREMENT,
  room_no    INT NOT NULL,
  user_no    INT NOT NULL,
  content    TEXT NOT NULL,
  create_date DATETIME NOT NULL,
  message_type VARCHAR(20) NOT NULL,
  PRIMARY KEY (message_no),
  KEY fk_msg_room (room_no),
  KEY fk_msg_user (user_no),
  CONSTRAINT fk_msg_room FOREIGN KEY (room_no) REFERENCES TB_CHAT_ROOM (room_no),
  CONSTRAINT fk_msg_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no)
);

CREATE TABLE IF NOT EXISTS TB_ATTACHMENT (
  attachment_no VARCHAR(36) NOT NULL COMMENT 'UUID',
  message_no    INT NOT NULL COMMENT 'FK',
  original_name VARCHAR(100) NOT NULL,
  file_name     VARCHAR(100) NOT NULL,
  path          VARCHAR(300) NOT NULL,
  size          INT NOT NULL,
  content_type  VARCHAR(100) NOT NULL,
  extension     VARCHAR(10)  NOT NULL DEFAULT 'PDF,JPG',
  create_date   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delete_date   DATETIME DEFAULT NULL,
  PRIMARY KEY (attachment_no),
  KEY fk_att_msg (message_no),
  CONSTRAINT fk_att_msg FOREIGN KEY (message_no) REFERENCES TB_CHAT_MESSAGE (message_no)
);

CREATE TABLE IF NOT EXISTS TB_ALARM (
  alarm_no  INT NOT NULL AUTO_INCREMENT COMMENT '자동증가',
  user_no   INT NOT NULL COMMENT 'FK',
  alarm_type VARCHAR(20) NOT NULL,
  reference_no INT NOT NULL,
  content   TEXT NOT NULL,
  is_read   CHAR(1) NOT NULL DEFAULT 'N',
  create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sender_no INT DEFAULT NULL,
  PRIMARY KEY (alarm_no),
  KEY fk_alarm_user (user_no),
  CONSTRAINT fk_alarm_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no)
);

CREATE TABLE IF NOT EXISTS TB_MEETING_ROOM (
  room_no INT NOT NULL AUTO_INCREMENT,
  team_no INT DEFAULT NULL,
  title   VARCHAR(100) NOT NULL,
  scheduled_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  scheduled_end_time DATETIME DEFAULT NULL,
  started_time DATETIME DEFAULT NULL,
  ended_time   DATETIME DEFAULT NULL,
  room_code VARCHAR(255) NOT NULL,
  create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_date DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  cal_detail_no INT DEFAULT NULL COMMENT '생성된 달력상세 PK',
  PRIMARY KEY (room_no),
  UNIQUE KEY UK_MEETING_ROOM_CODE (room_code),
  KEY idx_mr_scheduled (scheduled_time),
  KEY idx_mr_scheduled_end (scheduled_end_time),
  KEY idx_mr_team_no (team_no),
  KEY idx_mr_cal_detail (cal_detail_no),
  CONSTRAINT fk_mr_team FOREIGN KEY (team_no) REFERENCES TB_TEAM (team_no) ON DELETE SET NULL,
  CONSTRAINT ck_mr_range CHECK (scheduled_end_time IS NULL OR scheduled_end_time >= scheduled_time)
);

CREATE TABLE IF NOT EXISTS TB_MEETING_PARTICIPANT (
  room_no INT NOT NULL,
  role_no VARCHAR(4) NOT NULL,
  user_no INT NOT NULL,
  join_time DATETIME DEFAULT NULL,
  out_time  DATETIME DEFAULT NULL,
  PRIMARY KEY (room_no, user_no),
  KEY idx_mp_role (role_no),
  KEY idx_mp_user (user_no),
  CONSTRAINT fk_mp_role FOREIGN KEY (role_no) REFERENCES TB_ROLE_CODE (group_no),
  CONSTRAINT fk_mp_room FOREIGN KEY (room_no) REFERENCES TB_MEETING_ROOM (room_no) ON DELETE CASCADE,
  CONSTRAINT fk_mp_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS TB_MEETING_SUMMARY (
  room_no INT NOT NULL,
  summary      LONGTEXT DEFAULT NULL,
  action_items LONGTEXT DEFAULT NULL,
  decisions    LONGTEXT DEFAULT NULL,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (room_no),
  CONSTRAINT fk_ms_room FOREIGN KEY (room_no) REFERENCES TB_MEETING_ROOM (room_no) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS TB_MEETING_TRANSCRIPT (
  room_no INT NOT NULL,
  merged_text LONGTEXT DEFAULT NULL,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (room_no),
  CONSTRAINT fk_mt_room FOREIGN KEY (room_no) REFERENCES TB_MEETING_ROOM (room_no) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS TB_CALENDAR (
  cal_no  INT NOT NULL AUTO_INCREMENT,
  team_no INT NOT NULL,
  user_no INT NOT NULL,
  PRIMARY KEY (cal_no),
  KEY fk_cal_team (team_no),
  KEY fk_cal_user (user_no),
  CONSTRAINT fk_cal_team FOREIGN KEY (team_no) REFERENCES TB_TEAM (team_no),
  CONSTRAINT fk_cal_user FOREIGN KEY (user_no) REFERENCES TB_MEMBER (user_no)
);

CREATE TABLE IF NOT EXISTS TB_CALENDAR_DETAIL (
  cal_detail_no INT NOT NULL AUTO_INCREMENT,
  cal_no        INT NOT NULL,
  contents      TEXT DEFAULT NULL,
  start_date    DATE NOT NULL,
  start_time    TIME DEFAULT NULL,
  end_date      DATE NOT NULL,
  end_time      TIME DEFAULT NULL,
  delete_yn     CHAR(1) NOT NULL DEFAULT 'N',
  create_date   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reg_user_no   INT NOT NULL,
  update_dt     DATETIME DEFAULT NULL,
  update_user_no INT DEFAULT NULL,
  title         VARCHAR(100) NOT NULL,
  PRIMARY KEY (cal_detail_no),
  KEY fk_cd_cal (cal_no),
  KEY fk_cd_reg_user (reg_user_no),
  KEY fk_cd_update_user (update_user_no),
  CONSTRAINT fk_cd_cal FOREIGN KEY (cal_no) REFERENCES TB_CALENDAR (cal_no),
  CONSTRAINT fk_cd_reg_user FOREIGN KEY (reg_user_no) REFERENCES TB_MEMBER (user_no),
  CONSTRAINT fk_cd_update_user FOREIGN KEY (update_user_no) REFERENCES TB_MEMBER (user_no)
);

CREATE TABLE IF NOT EXISTS TB_CALENDAR_DETAIL_PARTICIPANT (
  cal_detail_no INT NOT NULL,
  user_no       INT NOT NULL,
  PRIMARY KEY (cal_detail_no, user_no),
  KEY idx_cdp_user_no (user_no),
  CONSTRAINT fk_cdp_cal_detail FOREIGN KEY (cal_detail_no) REFERENCES TB_CALENDAR_DETAIL (cal_detail_no) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_cdp_user       FOREIGN KEY (user_no)       REFERENCES TB_MEMBER (user_no) ON UPDATE CASCADE
);
