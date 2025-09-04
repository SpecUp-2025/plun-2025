package com.spec.plun.team.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.team.dto.TeamAcceptRequest;
import com.spec.plun.team.dto.TeamCreateRequest;
import com.spec.plun.team.dto.TeamCreateResponse;
import com.spec.plun.team.dto.TeamDeleteRequest;
import com.spec.plun.team.dto.TeamMemberDTO;
import com.spec.plun.team.dto.TeamsInvitationDTO;
import com.spec.plun.team.service.TeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

	private final TeamService teamService;

	/** 팀별 회원 목록 */
    @GetMapping("/{teamNo}/members")
    public ResponseEntity<List<TeamMemberDTO>> listByTeam(@PathVariable("teamNo") Integer teamNo) {
        return ResponseEntity.ok(teamService.listMembersByTeam(teamNo));
    }
    
    
    @GetMapping("/teamList/{userNo}")
    public ResponseEntity<List<TeamMemberDTO>> teamList(@PathVariable("userNo") Integer userNo) {
        return ResponseEntity.ok(teamService.teamList(userNo));
    }
    
    @PostMapping("/createTeam")
    public ResponseEntity<Object> createTeam( @RequestBody TeamCreateRequest teamCreateRequest) {
    	TeamCreateResponse teamCreateResponse = teamService.createTeam(teamCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(teamCreateResponse);
    }
    
    
    @GetMapping("/teamDetail/{teamNo}")
    public ResponseEntity<Object> teamDetail(@PathVariable("teamNo") Integer teamNo) {
    	Map<String, Object> map = teamService.teamDetail(teamNo); 
        return ResponseEntity.ok(Map.of("list",map));
    }
    
    @PutMapping("/teamDelete")
    public ResponseEntity<Object> teamDelete(@RequestBody TeamDeleteRequest teamDeleteRequest){
    	return ResponseEntity.ok(teamService.teamDelete(teamDeleteRequest));
    }
    
    @GetMapping("/invitation/{userNo}")
    public ResponseEntity<List<TeamsInvitationDTO>> invitation(@PathVariable ("userNo") Integer userNo){
    	return ResponseEntity.ok(teamService.invitation(userNo));
    }
    
    
    @PostMapping("/accept")
    public ResponseEntity<Object> accept(@RequestBody TeamAcceptRequest accept){
    	teamService.accept(accept);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/cancel/{invitedId}")
    public ResponseEntity<Object> cancel(@PathVariable("invitedId") Integer invitedId){
    	teamService.cancel(invitedId);
        return ResponseEntity.noContent().build();
    }
}
