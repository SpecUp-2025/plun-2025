package com.spec.plun.team.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.team.dto.TeamCreateRequest;
import com.spec.plun.team.dto.TeamMemberDTO;



@Mapper
public interface TeamDAO {

    
	List<TeamMemberDTO> selectByTeam(Integer teamNo);

	List<TeamMemberDTO> teamList(Integer userNo);

	void createTeamLeader(@Param("userNo") Integer userNo, @Param("teamNo")Integer teamNo);

	void insertInvite(@Param("teamNo")Integer teamNo, @Param("userNo")Integer userNo, @Param("inviteEmail")String inviteEmail);

	void createTeam(TeamCreateRequest teamCreateRequest);
}
