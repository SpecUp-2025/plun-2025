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
('D001', '예약'),
('D002', '시작'),
('D003', '종료'),
('E001', '업로드'),
('E002', '진행 중'),
('E003', '완료'),
('E004', '실패');


INSERT INTO TB_MEMBER (role, email, password, name, delete_yn, delete_date, create_date, update_date) VALUES
('A001', 'admin1@example.com', 'pass1234', '관리자1', 'N', NULL, NOW(), NULL),
('A002', 'user1@example.com', 'pass1234', '회원1', 'N', NULL, NOW(), NULL),
('A002', 'user2@example.com', 'pass1234', '회원2', 'N', NULL, NOW(), NULL),
('A002', 'user3@example.com', 'pass1234', '회원3', 'N', NULL, NOW(), NULL),