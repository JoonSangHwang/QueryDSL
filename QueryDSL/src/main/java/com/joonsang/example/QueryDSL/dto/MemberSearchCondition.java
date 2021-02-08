package com.joonsang.example.QueryDSL.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    private String username;    // 회원명
    private String teamName;    // 팀명
    private Integer ageGoe;     // 나이
    private Integer ageLoe;     // 나이
}
