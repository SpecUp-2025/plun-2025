package com.spec.plun.member.DAO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spec.plun.member.DTO.MemberDTO;

@Mapper
public interface MemberDAO {

	MemberDTO getEmail(String email);

	Integer findEmail(String email);

	int register(MemberDTO memberDTO);

	boolean resetPassword(MemberDTO memberDTO);

	String getPassword(String email);

	int userDelete(Integer userNo);

	int countByEmail(String email);

	void insertSocialEmail(@Param("email")String email, @Param("name")String name);

	int socialRegister(MemberDTO memberDTO);

	Integer findtype(String email);

}
