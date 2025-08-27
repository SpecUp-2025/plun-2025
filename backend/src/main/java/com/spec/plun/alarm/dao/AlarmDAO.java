package com.spec.plun.alarm.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.alarm.entity.Alarm;

@Mapper
public interface AlarmDAO {

    void insertAlarm(Alarm alarm);

    List<Alarm> selectAlarmsByUserNo(int userNo);
    
    // AlarmDAO 인터페이스에 추가
    String selectUserNameByUserNo(int userNo);

    void updateAlarmIsRead(@Param("alarmNo") int alarmNo);
}
