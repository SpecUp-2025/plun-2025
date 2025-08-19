package com.spec.plun.auth.service;

import org.springframework.stereotype.Service;

import com.spec.plun.auth.DAO.QualificationDAO;
import com.spec.plun.auth.DTO.LoginDTO;
import com.spec.plun.auth.DTO.QualificationCheckDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QualificationService {
	
	private final QualificationDAO qualificationDAO;
	
	
	public QualificationCheckDTO getByEmail(LoginDTO member) {
		return qualificationDAO.getByEmail(member.getEmail());
	}

}