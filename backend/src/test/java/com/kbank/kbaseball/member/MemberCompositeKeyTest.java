package com.kbank.kbaseball.member;

import com.kbank.kbaseball.domain.team.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberCompositeKeyTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("복합키 Member 저장 시 IDENTITY id가 할당되고 정상 조회되어야 한다")
    void save_shouldPersistWithCompositeKeyAndBeRetrievable() {
        Member member = Member.builder()
                .name("테스트유저")
                .supportTeam(Team.LG)
                .telegramId("99999")
                .notifyGameAnalysis(false)
                .notifyRainAlert(false)
                .notifyRealTimeAlert(false)
                .build();

        Member saved = memberRepository.save(member);

        assertThat(saved.getSeqId()).isNotNull();
        assertThat(saved.getTelegramId()).isEqualTo("99999");

        Optional<Member> found = memberRepository.findByLongId(saved.getSeqId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트유저");
        assertThat(found.get().getTelegramId()).isEqualTo("99999");
    }

    @Test
    @DisplayName("isNew()는 id가 null일 때만 true를 반환해야 한다")
    void isNew_shouldBeTrueOnlyWhenIdIsNull() {
        Member newMember = Member.builder()
                .name("신규유저")
                .supportTeam(Team.HH)
                .telegramId("11111")
                .build();

        assertThat(newMember.isNew()).isTrue();

        Member saved = memberRepository.save(newMember);
        assertThat(saved.isNew()).isFalse();
    }
}
