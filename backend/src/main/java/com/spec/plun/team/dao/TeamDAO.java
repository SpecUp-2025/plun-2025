package com.spec.plun.team.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.team.dto.TeamMemberDTO;

@Mapper
public interface TeamDAO {

    
	List<TeamMemberDTO> selectByTeam(Integer teamNo);

	List<TeamMemberDTO> teamList(Integer userNo);
}
