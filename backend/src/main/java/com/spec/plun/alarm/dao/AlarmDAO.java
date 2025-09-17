package com.spec.plun.alarm.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.alarm.entity.Alarm;

@Mapper
public interface AlarmDAO {

    void insertAlarm(Alarm alarm);

    List<Alarm> selectAlarmsByUserNo(Integer userNo);
    
    String selectUserNameByUserNo(Integer userNo);

    void updateAlarmIsRead(@Param("alarmNo") Integer alarmNo);
}
