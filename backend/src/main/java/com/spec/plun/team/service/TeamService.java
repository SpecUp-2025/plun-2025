package com.spec.plun.team.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spec.plun.team.dao.TeamDAO;
import com.spec.plun.team.dto.TeamMemberDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final TeamDAO teamDAO;

	public List<TeamMemberDTO> listMembersByTeam(Integer teamNo) {
		return teamDAO.selectByTeam(teamNo);
	}
}
