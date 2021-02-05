package com.joonsang.example.QueryDSL;

import com.joonsang.example.QueryDSL.entity.Member;
import com.joonsang.example.QueryDSL.entity.QMember;
import com.joonsang.example.QueryDSL.entity.QTeam;
import com.joonsang.example.QueryDSL.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import java.util.List;

import static com.joonsang.example.QueryDSL.entity.QMember.*;
import static com.joonsang.example.QueryDSL.entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryDslApplicationTests {

	@PersistenceContext
	EntityManager em;

	@PersistenceUnit
	EntityManagerFactory emf;

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

		log(result);
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

		log(result);

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
	@DisplayName("Join: TeamA 에 속한 멤버를 구하라")
	public void join() throws Exception {
		QMember member = QMember.member;
		QTeam team = QTeam.team;

		List<Member> result = queryFactory
				.selectFrom(member)
				.join(member.team, team)
				.where(team.name.eq("teamA"))
				.fetch();

		log(result);

		assertThat(result)
				.extracting("username")
				.containsExactly("member1", "member2");
	}

	/**
	 * 세타 조인(연관관계가 없는 필드로 조인)
	 */
	@Test
	@DisplayName("세타 Join: 회원의 이름이 팀 이름과 같은 회원 조회")
	public void theta_join() throws Exception {
		List<Member> result = queryFactory
				.select(member)
				.from(member, team)
				.where(member.username.eq(team.name))
				.fetch();

		log(result);

		assertThat(result)
				.extracting("username")
				.containsExactly("teamA", "teamB");
	}

	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	 * JPQL	: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
	 * SQL	: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
	 **/
	@Test
	@DisplayName("left Join: on절")
	public void join_on_filtering() throws Exception {
		List<Tuple> result = queryFactory
				.select(member, team)
				.from(member)
				.leftJoin(member.team, team).on(team.name.eq("teamA"))
				.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}


	/**
	 * 2. 연관관계 없는 엔티티 외부 조인
	 * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
	 * JPQL	: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
	 * SQL	: SELECT m.*, t.* FROM  Member m LEFT JOIN Team t ON m.username = t.name
	 **/
	@Test
	@DisplayName("세타 Join: on절")
	public void join_on_no_relation() throws Exception {
		List<Tuple> result = queryFactory
				.select(member, team)
				.from(member)
				.leftJoin(team).on(member.username.eq(team.name))
				.fetch();

		for (Tuple tuple : result) {
			System.out.println("t=" + tuple);
		}
	}


	@Test
	@DisplayName("페치조인: 미적용")
	public void fetchJoinNo() throws Exception {
		em.flush();
		em.clear();
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1"))
				.fetchOne();

		// 로딩 초기화 여부
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

	@Test
	@DisplayName("페치조인: 적용")
	public void fetchJoinUse() throws Exception {
		em.flush();
		em.clear();

		Member findMember = queryFactory
				.selectFrom(member)
				.join(member.team, team).fetchJoin()
				.where(member.username.eq("member1"))
				.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("페치 조인 적용").isTrue();
	}

	@Test
	@DisplayName("서브쿼리: 나이가 가장 많은 회원 조회")
	public void subQuery() throws Exception {
		QMember memberSub = new QMember("memberSub");

		List<Member> result = queryFactory
				.selectFrom(member)
				.where(member.age.eq(
				JPAExpressions
						.select(memberSub.age.max())
						.from(memberSub)
				))
				.fetch();

		assertThat(result)
				.extracting("age")
				.containsExactly(90);
	}

	@Test
	@DisplayName("서브쿼리: Select 절 안에 subquery")
	public void subQuery2() throws Exception {
		QMember memberSub = new QMember("memberSub");

		List<Tuple> fetch = queryFactory
				.select(member.username,
						JPAExpressions
								.select(memberSub.age.avg())
								.from(memberSub)
				).from(member)
				.fetch();

		for (Tuple tuple : fetch) {
			System.out.println("username = " + tuple.get(member.username));
			System.out.println("age = " + tuple.get(JPAExpressions.select(memberSub.age.avg()).from(memberSub)));
		}
	}

	@Test
	@DisplayName("case문")
	public void case1() throws Exception {
		List<String> result = queryFactory
				.select(member.age
						.when(10).then("열살")
						.when(20).then("스무살")
						.otherwise("기타"))
				.from(member)
				.fetch();
	}

	@Test
	@DisplayName("case문")
	public void case2() throws Exception {
		List<String> result = queryFactory
				.select(new CaseBuilder()
						.when(member.age.between(0, 20)).then("0~20살")
						.when(member.age.between(21, 30)).then("21~30살")
						.otherwise("기타"))
				.from(member)
				.fetch();
	}

	/**
	 * 임의의 순서로 회원을 출력하고 싶다면?
	 * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
	 * 2. 0 ~ 20살 회원 출력
	 * 3. 21 ~ 30살 회원 출
	 */
	@Test
	@DisplayName("case문: 임의 순서")
	public void case3() throws Exception {
		NumberExpression<Integer> rankPath = new CaseBuilder()
				.when(member.age.between(0, 20)).then(2)
				.when(member.age.between(21, 30)).then(1)
				.otherwise(3);

		List<Tuple> result = queryFactory
				.select(member.username, member.age, rankPath)
				.from(member)
				.orderBy(rankPath.desc())
				.fetch();

		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			Integer rank = tuple.get(rankPath);
			System.out.println("username = " + username + " age = " + age + " rank = " + rank);
		}
	}














	@Test
	void contextLoads() {
	}

	public void log(List<Member> result) {
		System.out.println("-------------------------------------------");
		System.out.println("-------------------------------------------");
		for (Member m : result) {
			System.out.println("result=" + m + "		-> Team = " + m.getTeam());
		}
		System.out.println("-------------------------------------------");
		System.out.println("-------------------------------------------");
	}
}
