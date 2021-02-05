package com.joonsang.example.QueryDSL;

import com.joonsang.example.QueryDSL.entity.Member;
import com.joonsang.example.QueryDSL.entity.QMember;
import com.joonsang.example.QueryDSL.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.util.List;

import static com.joonsang.example.QueryDSL.entity.QMember.*;
import static com.joonsang.example.QueryDSL.entity.QTeam.team;
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
	@DisplayName("검색조건: member1을 찾아라")
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
	@DisplayName("검색조건: 이름이 member1 이면서 나이가 10살인 멤버")
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
	@DisplayName("조회결과: List Detail First Count")
	public void searchResult() {
		// 리스트 조회
		List<Member> fetch = queryFactory
				.selectFrom(member)
				.fetch();

		// 단 건
		Member findMember1 = queryFactory
				.selectFrom(member)
				.where(member.age.eq(10))
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

	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 *    단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
	 */
	@Test
	@DisplayName("정렬: Order by")
	public void sort() {

		List<Member> result = queryFactory
				.selectFrom(member)
				.orderBy(member.age.desc(), member.username.asc().nullsLast())
				.fetch();

		for (Member m : result)
			System.out.println("result=" + m + "		-> Team = " + m.getTeam());
	}

	@Test
	@DisplayName("페이징: Offset Limit")
	public void paging1() {
		List<Member> result = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1) 	//0부터 시작(zero index)
				.limit(2) 	//최대 2건 조회
				.fetch();

		for (Member m : result)
			System.out.println("result=" + m + "		-> Team = " + m.getTeam());

		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	@DisplayName("페이징: 카운트 쿼리와 함께 전체 조회")
	public void paging2() {
		QueryResults<Member> queryResults = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1)
				.limit(2)
				.fetchResults();

		assertThat(queryResults.getTotal()).isGreaterThan(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);
	}

	@Test
	@DisplayName("집합: SUM AVG MAX MIN")
	public void aggregation() throws Exception {
		List<Tuple> result = queryFactory
				.select(member.count(),		// 카운트
						member.age.sum(),	// 합
						member.age.avg(),	// 평균
						member.age.max(),	// 최대
						member.age.min())	// 최소
				.from(member)
				.fetch();
		
		for (Tuple m : result)
			System.out.println("result=" + m);

		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isGreaterThan(4);
		assertThat(tuple.get(member.age.sum())).isGreaterThan(10);
		assertThat(tuple.get(member.age.avg())).isGreaterThan(25);
		assertThat(tuple.get(member.age.max())).isGreaterThan(40);
		assertThat(tuple.get(member.age.min())).isGreaterThan(9);
	}

	@Test
	@DisplayName("Group by: 팀의 이름과 각 팀의 평균 연령")
	public void group() throws Exception {
		List<Tuple> result = queryFactory
				.select(team.name, member.age.avg())
				.from(member)
				.join(member.team, team)
				.groupBy(team.name)
				.fetch();

		for (Tuple m : result)
			System.out.println("result=" + m);

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isGreaterThan(9);
		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isGreaterThan(9);
	}






	@Test
	void contextLoads() {
	}

}
