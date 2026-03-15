package com.kbank.kbaseball.member;

import com.kbank.kbaseball.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByNotifyRealTimeAlertTrue();

    List<Member> findBySupportTeamAndNotifyRainAlertTrue(Team supportTeam);

    List<Member> findByNotifyGameAnalysisTrue();

    List<Member> findAllBySupportTeam(Team supportTeam);

    Optional<Member> findByNameAndSupportTeamAndTelegramId(String name, Team supportTeam, String telegramId);

    Optional<Member> findByNameAndSupportTeam(String name, Team supportTeam);
}
