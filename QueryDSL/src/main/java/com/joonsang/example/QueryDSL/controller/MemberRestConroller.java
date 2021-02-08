package com.joonsang.example.QueryDSL.controller;

import com.joonsang.example.QueryDSL.dto.MemberSearchCondition;
import com.joonsang.example.QueryDSL.dto.MemberTeamDto;
import com.joonsang.example.QueryDSL.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MemberRestConroller {

    @Autowired
    MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberRepository.search(condition);
    }

}
