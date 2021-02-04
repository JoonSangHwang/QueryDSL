package com.joonsang.example.QueryDSL;

import com.joonsang.example.QueryDSL.entity.Member;
import com.joonsang.example.QueryDSL.entity.QMember;
import com.joonsang.example.QueryDSL.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import static com.joonsang.example.QueryDSL.entity.QMember.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryDslApplicationTests {

	@PersistenceContext
	EntityManager em;

	JPAQueryFactory queryFactory;

	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
		
		// 기초 데이터
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	@Test
	@DisplayName("JPQL 사용 => member1을 찾아라")
	public void startJPQL() {
		String qlString = "select m from Member m " +
						  "where m.username = :username";

		Member findMember = em.createQuery(qlString, Member.class)
				.setParameter("username", "member1")
				.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	@DisplayName("QueryDSL 사용 => member1을 찾아라")
	public void startQuerydsl() {

		/**
		 * Q클래스 인스턴스를 사용하는 2가지 방법
		 *
		 *   1. 별칭 직접 지정
		 *   2. 기본 인스턴스 사용
		 *   3. 기본 인스턴스의 import static 사용 (추천)
		 */

//		QMember m = new QMember("m");	// 1. 별칭 직접 지정
//		QMember m = QMember.member;		// 2. 기본 인스턴스 사용

		// 3. 기본 인스턴스의 import static 사용
		Member findMember = queryFactory
				.select(member)
				.from(member)
				.where(member.username.eq("member1"))
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}


	@Test
	void contextLoads() {
	}

}
