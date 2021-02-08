package com.joonsang.example.QueryDSL.repository;
import com.joonsang.example.QueryDSL.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

}
