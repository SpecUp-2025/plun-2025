INSERT INTO TB_ROLE_CODE (group_no, group_name) VALUES
('A001', '관리자'),
('A002', '회원'),
('B001', '팀장'),
('B002', '팀원'),
('C001', '방장'),
('C002', '참여자'),
('F001','대기'),
('F002','수락'),
('F003','거절');


INSERT INTO TB_MEMBER_LOGIN (login_type_name) VALUES
('local'),('social');

INSERT INTO TB_MEMBER (role, email, password, name, login_type_no) VALUES
('A002', 'user1@example.com', '$2a$12$Zs6qLzJhxFRPYVXbmoBFdebERT0.ckmBngst.gffugbjnWlLEq0Dm', '신짱구', 1),
('A002', 'user2@example.com', '$2a$12$Zs6qLzJhxFRPYVXbmoBFdebERT0.ckmBngst.gffugbjnWlLEq0Dm', '봉미선', 1),
('A002', 'user3@example.com', '$2a$12$Zs6qLzJhxFRPYVXbmoBFdebERT0.ckmBngst.gffugbjnWlLEq0Dm', '신형만', 1),
('A002', 'user4@example.com', '$2a$12$Zs6qLzJhxFRPYVXbmoBFdebERT0.ckmBngst.gffugbjnWlLEq0Dm', '훈이', 1),
('A002', 'user5@example.com', '$2a$12$Zs6qLzJhxFRPYVXbmoBFdebERT0.ckmBngst.gffugbjnWlLEq0Dm', '철수', 1);