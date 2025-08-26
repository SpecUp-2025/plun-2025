package com.spec.plun.team.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spec.plun.team.dto.TeamMemberDTO;
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
}
