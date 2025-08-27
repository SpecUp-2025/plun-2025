package com.spec.plun.team.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.spec.plun.email.service.EmailSendInviteService;
import com.spec.plun.team.dao.TeamDAO;
import com.spec.plun.team.dto.TeamCreateRequest;
import com.spec.plun.team.dto.TeamCreateResponse;
import com.spec.plun.team.dto.TeamMemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
	
	private final TeamDAO teamDAO;
	private final EmailSendInviteService emailSendInviteService;

	public List<TeamMemberDTO> listMembersByTeam(Integer teamNo) {
		return teamDAO.selectByTeam(teamNo);
	}

	public List<TeamMemberDTO> teamList(Integer userNo) {
		return teamDAO.teamList(userNo);
	}

	@Transactional
	public TeamCreateResponse createTeam(TeamCreateRequest teamCreateRequest) {
		teamDAO.createTeam(teamCreateRequest);
		
		teamDAO.createTeamLeader(teamCreateRequest.getUserNo(),teamCreateRequest.getTeamNo());
		
		for(String inviteEmail : teamCreateRequest.getInvite()) {
			teamDAO.insertInvite(teamCreateRequest.getTeamNo(),teamCreateRequest.getUserNo(),inviteEmail);
		}
		
		TransactionSynchronizationManager.registerSynchronization((new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				for(String inviteEmail : teamCreateRequest.getInvite() ) {
					try {
						emailSendInviteService.sendSimpleMessage(inviteEmail,teamCreateRequest.getEmail(),teamCreateRequest.getUserName());
					} catch (Exception e) {
						log.warn("Invite mail failed. teamNo={}, email={}", teamCreateRequest.getTeamNo(), inviteEmail, e);
					}
				}
			}
		}));
		
		
		return new TeamCreateResponse(teamCreateRequest.getTeamName(),teamCreateRequest.getTeamNo());
	}
}
