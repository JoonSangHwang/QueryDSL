package com.joonsang.example.QueryDSL.repository;

import com.joonsang.example.QueryDSL.dto.MemberSearchCondition;
import com.joonsang.example.QueryDSL.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition);
    public List<MemberTeamDto> search(MemberSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageSimple3(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

}
