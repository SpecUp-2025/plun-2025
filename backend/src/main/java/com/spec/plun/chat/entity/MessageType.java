package com.spec.plun.chat.entity;

public enum MessageType {
    ENTER,      // 유저 입장
    LEAVE,      // 유저 퇴장
    TALK,       // 일반 텍스트 메시지
    FILE,       // 파일 전송
    IMAGE,      // 이미지 전송
    MENTION     // 멘션 포함 메시지
}
