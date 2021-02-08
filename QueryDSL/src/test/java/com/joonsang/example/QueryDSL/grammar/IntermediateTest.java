package com.joonsang.example.QueryDSL.grammar;

import com.joonsang.example.QueryDSL.dto.MemberDto;
import com.joonsang.example.QueryDSL.entity.Member;
import com.joonsang.example.QueryDSL.entity.QMember;
import com.joonsang.example.QueryDSL.entity.Team;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

import static com.joonsang.example.QueryDSL.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntermediateTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        // 기초 데이터 - 팀
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        // 기초데이터 - 멤버
        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));
        em.persist(new Member(null, 50, teamB));
        em.persist(new Member("member5", 60));
        em.persist(new Member("member6", 70));
        em.persist(new Member("teamA", 80));
        em.persist(new Member("teamB", 90));
    }

    @Test
    @DisplayName("Projection 1개")
    public void projection1() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        log(result, "string");
    }

    @Test
    @DisplayName("Projection 2개 이상")
    public void projection2() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }

    @Test
    @DisplayName("Projection JPQL 사용")
    public void projection3() throws Exception {
        List<MemberDto> result = em.createQuery(
                "select new com.joonsang.example.QueryDSL.dto.MemberDto(m.username, m.age) " +
                        "from Member m", MemberDto.class)
                .getResultList();

        log(result, "memberDto");
    }

    @Test
    @DisplayName("Projection QueryDSL 사용 (프로퍼티) 1/3")
    public void projection4() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        log(result, "memberDto");
    }

    @Test
    @DisplayName("Projection QueryDSL 사용 (필드) 2/3")
    public void projection5() throws Exception {
        // 기본 생성자 없이 필드에 Setter로 바로 주입
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        log(result, "memberDto");
    }

    @Test
    @DisplayName("Projection QueryDSL 사용 (필드) 2/3 -- 별칭이 다를 때")
    public void projection6() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")))
                .from(member)
                .fetch();

        log(result, "memberDto");
    }

    @Test
    @DisplayName("Projection QueryDSL 사용 (생성자) 3/3")
    public void projection7() throws Exception {
        // 생성자는 프로퍼티의 타입이 일치해야 들어감
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        log(result, "memberDto");
    }














    public void log(List<?> result, String type) {
        System.out.println("-------------------------------------------");
        System.out.println("-------------------------------------------");

        if ("member".equals(type)) {
            for (Object m : result) {
                Member mm = (Member) m;
                System.out.println("result=" + mm + "		-> Team = " + mm.getTeam());
            }
        }

        if ("string".equals(type)) {
            for (Object s : result) {
                System.out.println("result=" + s);
            }
        }

        if ("memberDto".equals(type)) {
            for (Object m : result) {
                MemberDto mm = (MemberDto) m;
                System.out.println("result=" + mm);
            }
        }



        System.out.println("-------------------------------------------");
        System.out.println("-------------------------------------------");
    }

}
