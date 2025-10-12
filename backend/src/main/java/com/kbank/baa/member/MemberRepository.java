package com.kbank.baa.member;

import com.kbank.baa.admin.Team;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByNotifyRealTimeAlertTrue();

    List<Member> findBySupportTeamAndNotifyRainAlertTrue(Team supportTeam);

    List<Member> findByNotifyGameAnalysisTrue();

    List<Member> findAllBySupportTeam(Team supportTeam);

    Optional<Member> findByNameAndSupportTeamAndTelegramId(String name, Team supportTeam, String telegramId);

    Optional<Member> findByNameAndSupportTeam(String name, Team supportTeam);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM Member m WHERE m.telegramId IS NULL AND m.createdAt < :before")
    int deleteOldPending(@Param("before") Instant before);
}
