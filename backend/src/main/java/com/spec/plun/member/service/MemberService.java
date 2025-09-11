package com.spec.plun.member.service;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.spec.plun.member.DAO.MemberDAO;
import com.spec.plun.member.DTO.MemberDTO;
import com.spec.plun.member.DTO.SetPasswordRequest;
import com.spec.plun.team.dao.TeamDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final PasswordEncoder passwordEncoder;
	
	private final MemberDAO memberDAO;
	private final TeamDAO teamDAO;
	
	public MemberDTO getEmail(String email) {
		return memberDAO.getEmail(email);
	}

	public boolean findEmail(String email) {
		return memberDAO.findEmail(email)== 1;
	}

	public boolean register(MemberDTO memberDTO) {
		return memberDAO.register(memberDTO) == 1;
	}

	
	public boolean resetPassword(MemberDTO memberDTO) {
		return memberDAO.resetPassword(memberDTO);
		
	}

	public void setPassword(SetPasswordRequest setPasswordRequest) {
		String currentPass = memberDAO.getPassword(setPasswordRequest.getEmail());
		
		 if (!passwordEncoder.matches(setPasswordRequest.getCurrentPassword(), currentPass)) {
		     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
		    }
		if(passwordEncoder.matches(setPasswordRequest.getPassword(), currentPass)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호가 기존과 같습니다.");
        }
		
		String hashed = passwordEncoder.encode(setPasswordRequest.getPassword());
		MemberDTO memberDTO = new MemberDTO();
		memberDTO.setEmail(setPasswordRequest.getEmail());
		memberDTO.setPassword(hashed);
		memberDAO.resetPassword(memberDTO);
	}

	@Transactional
	public boolean userDelete(Integer userNo) {
		int cnt = teamDAO.teamsDelete(userNo);
		int count = memberDAO.userDelete(userNo);
		
		return count+cnt >= 1;
	}

	public int countByEmail(String email) {
		return memberDAO.countByEmail(email);
	}

	public void insertSocialEmail(String email, String name) {
		memberDAO.insertSocialEmail(email,name);
		
	}

	public int socialRegister(MemberDTO memberDTO) {
		return memberDAO.socialRegister(memberDTO);
		
	}

	public Integer findtype(String email) {
		return memberDAO.findtype(email);
	}

}
