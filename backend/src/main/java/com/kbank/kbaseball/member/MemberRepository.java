package com.kbank.kbaseball.member;

import com.kbank.kbaseball.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByLongId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM Member m WHERE m.id = :id")
    void deleteByLongId(@Param("id") Long id);

    List<Member> findByNotifyRealTimeAlertTrue();

    List<Member> findBySupportTeamAndNotifyRainAlertTrue(Team supportTeam);

    List<Member> findByNotifyGameAnalysisTrue();

    List<Member> findAllBySupportTeam(Team supportTeam);

    Optional<Member> findByNameAndSupportTeamAndTelegramId(String name, Team supportTeam, String telegramId);

    boolean existsByTelegramId(String telegramId);

    List<Member> findByNameAndSupportTeam(String name, Team supportTeam);
}
