package com.kbank.baa.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByNotifyRealTimeAlertTrue();

    List<Member> findBySupportTeamAndNotifyRainAlertTrue(Team supportTeam);

    List<Member> findByNotifyGameAnalysisTrue();
}
