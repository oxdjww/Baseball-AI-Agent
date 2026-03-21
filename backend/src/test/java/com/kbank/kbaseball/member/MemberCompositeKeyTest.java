package com.kbank.kbaseball.member;

import com.kbank.kbaseball.config.JpaConfig;
import com.kbank.kbaseball.domain.team.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class MemberSingleKeyTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("Member 저장 시 시퀀스 id가 할당되고 정상 조회되어야 한다")
    void save_shouldPersistWithSequenceIdAndBeRetrievable() {
        Member member = Member.builder()
                .name("테스트유저")
                .supportTeam(Team.LG)
                .telegramId("99999")
                .notifyGameAnalysis(false)
                .notifyRainAlert(false)
                .notifyRealTimeAlert(false)
                .build();

        Member saved = memberRepository.save(member);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTelegramId()).isEqualTo("99999");

        Optional<Member> found = memberRepository.findByLongId(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트유저");
        assertThat(found.get().getTelegramId()).isEqualTo("99999");
    }

    @Test
    @DisplayName("동일한 telegram_id로 두 번 저장 시 UNIQUE 제약 위반 예외가 발생해야 한다")
    void save_duplicateTelegramId_shouldThrowException() {
        memberRepository.save(Member.builder()
                .name("태태")
                .supportTeam(Team.LG)
                .telegramId("tae_dup_test")
                .build());
        memberRepository.flush();

        assertThatThrownBy(() -> {
            memberRepository.save(Member.builder()
                    .name("태태2")
                    .supportTeam(Team.KT)
                    .telegramId("tae_dup_test")
                    .build());
            memberRepository.flush();
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
