package com.joonsang.example.QueryDSL.repository;

import com.joonsang.example.QueryDSL.dto.MemberSearchCondition;
import com.joonsang.example.QueryDSL.dto.MemberTeamDto;
import com.joonsang.example.QueryDSL.dto.QMemberTeamDto;
import com.joonsang.example.QueryDSL.entity.Member;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.joonsang.example.QueryDSL.entity.QMember.member;
import static com.joonsang.example.QueryDSL.entity.QTeam.team;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public MemberRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    /**
     * 동적 쿼리와 성능 최적화
     * - Builder 사용
     */
    @Override
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼리와 성능 최적화
     * - Builder 사용
     */
    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {    return isEmpty(username) ? null : member.username.eq(username); }
    private BooleanExpression teamNameEq(String teamName) {    return isEmpty(teamName) ? null : team.name.eq(teamName); }
    private BooleanExpression ageGoe(Integer ageGoe) {    return ageGoe == null ? null : member.age.goe(ageGoe); }
    private BooleanExpression ageLoe(Integer ageLoe) {    return ageLoe == null ? null : member.age.loe(ageLoe); }


    /**
     * 단순한 페이징, fetchResults() 사용
     * - Querydsl 이 제공하는 fetchResults()를 사용하면 내용과 전체 카운트를 한번에 조회할 수 있다.
     * - 실제 쿼리는 2번 호출
     **/
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        // 리스트 쿼리 + 카운트 쿼리
        QueryResults<MemberTeamDto> results = jpaQueryFactory
                .select(
                        new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
//                .orderBy()                                /* fetchResult()는 카운트 쿼리 실행시 필요없는 order by는 제거 */
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();


        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 단순한 페이징, fetch() 사용
     * - 리스트 쿼리와 별개로 직접 토탈 카운트 쿼리 날림
     * - 카운트 쿼리 최적화 가능
     **/
    @Override
    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {
        // 리스트 쿼리
        List<MemberTeamDto> result = getMemberTeamDtos(condition, pageable);

        // 카운트 쿼리
        long cnt = getCnt(condition);

        return new PageImpl<>(result, pageable, cnt);
    }

    /**
     * 단순한 페이징, fetch() 사용
     * - CountQuery 최적화
     **/
    @Override
    public Page<MemberTeamDto> searchPageSimple3(MemberSearchCondition condition, Pageable pageable) {
        // 리스트 쿼리
        List<MemberTeamDto> result = getMemberTeamDtos(condition, pageable);

        // 카운트 쿼리
        JPAQuery<Member> countQuery = jpaQueryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        // 시작 페이지거나 마지막 페이지라면? 카운트 쿼리를 실행을 안함으로서 최적화 시킴
        return PageableExecutionUtils.getPage(result, pageable, () -> countQuery.fetchCount());
    }

    private long getCnt(MemberSearchCondition condition) {
        long cnt = jpaQueryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                ).fetchCount();
        return cnt;
    }

    private List<MemberTeamDto> getMemberTeamDtos(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> result = jpaQueryFactory
                .select(
                        new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return result;
    }


    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        return null;
    }
}