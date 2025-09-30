package com.spec.plun.alarm.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.alarm.entity.Alarm;
import com.spec.plun.alarm.service.AlarmService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/{userNo}")
    public ResponseEntity<List<Alarm>> getAlarms(@PathVariable("userNo") Integer userNo) {
        return ResponseEntity.ok(alarmService.getUserAlarms(userNo));
    }

    @PutMapping("/{alarmNo}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("alarmNo") Integer alarmNo) {
        alarmService.markAsRead(alarmNo);
        return ResponseEntity.ok().build();
    }
    @PostMapping
    public ResponseEntity<Void> createAlarm(@RequestBody Alarm alarm) {
        alarmService.insertAlarm(alarm);
        System.out.println("DB 삽입 후 생성된 alarmNo: " + alarm.getAlarmNo());
        return ResponseEntity.ok().build();
    }
}

