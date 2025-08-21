package com.spec.plun.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDTO {
    private Integer userNo;  // tb_member.user_no
    private String  name;    // tb_member.name
}
