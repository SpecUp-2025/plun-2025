package com.spec.plun.member.DAO;

import org.apache.ibatis.annotations.Mapper;

import com.spec.plun.member.DTO.MemberDTO;

@Mapper
public interface MemberDAO {

	MemberDTO getEmail(String email);

	int findEmail(String email);

	int register(MemberDTO memberDTO);

}
