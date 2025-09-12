package com.spec.plun.auth.DAO;

import org.apache.ibatis.annotations.Mapper;

import com.spec.plun.auth.DTO.QualificationCheckDTO;

@Mapper
public interface QualificationDAO {

	public QualificationCheckDTO getByEmail(String email);
}
