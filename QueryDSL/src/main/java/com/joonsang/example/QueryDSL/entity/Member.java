package com.joonsang.example.QueryDSL.entity;

import lombok.Getter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;


    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public Member(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }

    private String username;

    /**
     * JPA 표준 스펙 : 엔티티는 반드시 파라미터가 없는 생성자가 있어야 하고, 이는 public 또는 protected 여야 한다.
     */
    protected Member() {

    }

    public Member(String username) {
        this.username = username;
    }


    public void setUsername(String username) {
        this.username = username;
    }
}
