package com.spec.plun.meeting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DbHealthMapper {
	@Select("SELECT 1")
	Integer selectOne();
}
