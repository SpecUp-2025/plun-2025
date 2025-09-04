package com.spec.plun.team.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.email.service.EmailSendInviteService;
import com.spec.plun.team.dao.TeamDAO;
import com.spec.plun.team.dto.TeamAcceptRequest;
import com.spec.plun.team.dto.TeamCreateRequest;
import com.spec.plun.team.dto.TeamCreateResponse;
import com.spec.plun.team.dto.TeamDeleteRequest;
import com.spec.plun.team.dto.TeamMemberDTO;
import com.spec.plun.team.dto.TeamsInvitationDTO;

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

	public Object istTeam(Integer userNo) {
		return teamDAO.isTeam(userNo);
	}

	public Map<String, Object> teamDetail(Integer teamNo) {
		return teamDAO.teamDetail(teamNo);
	}

	public Object teamDelete(TeamDeleteRequest teamDeleteRequest) {
		return teamDAO.teamDelete(teamDeleteRequest);
	}

	public List<TeamsInvitationDTO> invitation(Integer userNo) {
		return teamDAO.invitation(userNo);
	}

	@Transactional
	public void accept(TeamAcceptRequest accept) {
		int u = teamDAO.acceptupdate(accept.getInvitedId());
		if (u != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상태 업데이트 실패");
		int i = teamDAO.accept(accept.getTeamNo(),accept.getUserNo());
		if (i != 1) throw new IllegalStateException("팀원 추가 실패");
	}

	public void cancel(Integer invitedId) {
		int u = teamDAO.cancel(invitedId);
		if (u != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상태 업데이트 실패");
	}
}
