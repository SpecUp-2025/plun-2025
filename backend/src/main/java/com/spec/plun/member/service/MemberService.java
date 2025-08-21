package com.spec.plun.member.service;


import org.springframework.stereotype.Service;

import com.spec.plun.member.DAO.MemberDAO;
import com.spec.plun.member.DTO.MemberDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {
	
	private final MemberDAO memberDAO;
	
	public MemberDTO getEmail(String email) {
		return memberDAO.getEmail(email);
	}

}
