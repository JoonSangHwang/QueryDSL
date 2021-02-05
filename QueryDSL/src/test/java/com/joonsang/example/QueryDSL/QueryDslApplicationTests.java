package com.joonsang.example.QueryDSL;

import com.joonsang.example.QueryDSL.entity.Member;
import com.joonsang.example.QueryDSL.entity.QMember;
import com.joonsang.example.QueryDSL.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.util.List;

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
		 *   2-1. 기본 인스턴스 사용
		 *   2-2. 기본 인스턴스의 import static 사용 (추천)
		 */

//		QMember m = new QMember("m");	// 1. 별칭 직접 지정
//		QMember m = QMember.member;		// 2-1. 기본 인스턴스 사용

		// 2-2. 기본 인스턴스의 import static 사용
		Member findMember = queryFactory
				.select(member)
				.from(member)
				.where(member.username.eq("member1"))
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	@DisplayName("QueryDSL 사용 => 이름이 member1 이면서 나이가 10살인 멤버를 찾아라")
	public void search() {
		Member findMember = queryFactory
				.selectFrom(member)
				.where(
						member.username.eq("member1") ,
						member.age.eq(10)
				)
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	@DisplayName("QueryDSL 사용 => 조회 결과")
	public void searchResult() {
		// 리스트 조회
		List<Member> fetch = queryFactory
				.selectFrom(member)
				.fetch();

		// 단 건
		Member findMember1 = queryFactory
				.selectFrom(member)
				.fetchOne();

		// 처음 한 건 조회
		Member findMember2 = queryFactory
				.selectFrom(member)
				.fetchFirst();

		// 페이징에서 사용
		QueryResults<Member> results = queryFactory
				.selectFrom(member)
				.fetchResults();

		// count 쿼리로 변경
		long count = queryFactory
				.selectFrom(member)
				.fetchCount();
	}


	@Test
	void contextLoads() {
	}

}
