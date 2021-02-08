package com.joonsang.example.QueryDSL.controller;

import com.joonsang.example.QueryDSL.dto.MemberSearchCondition;
import com.joonsang.example.QueryDSL.dto.MemberTeamDto;
import com.joonsang.example.QueryDSL.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MemberRestConroller {

    @Autowired
    MemberRepository memberRepository;

    //== http://localhost:70/v1/members?teamName=teamB&ageGoe=30
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition) {
        return memberRepository.search(condition);
    }

    //== http://localhost:70/v2/members?size=5&page=2
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    //== http://localhost:70/v3/members?size=5&page=2
    //== http://localhost:70/v3/members?size=0&page=20
    //== http://localhost:70/v3/members?size=0&page=200
    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }

}
