package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.NaverStandingsClient;
import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.KboTeamStandingDto;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberRepository;
import com.kbank.kbaseball.notification.telegram.TelegramNotificationClient;
import com.kbank.kbaseball.notification.telegram.dto.TelegramMessage;
import com.kbank.kbaseball.notification.telegram.dto.TelegramSendResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * StandingsAdjuster 통합 검증 테스트.
 *
 * 시나리오:
 *   경기 전 API가 반환하는 stale 순위 (경기 결과 미반영):
 *     1위 삼성  10승  5패  GB 0.0
 *     2위 LG     9승  6패  GB 0.5
 *     3위 KT     8승  7패  GB 1.0
 *
 *   실제 경기 결과: LG(원정) 5 : KT(홈) 3 → LG 승
 *
 *   보정 후 기댓값:
 *     1위 삼성  10승  5패  GB 0.0
 *     2위 LG    10승  6패  GB 0.5  (승률 10/16 = 0.625)
 *     3위 KT     8승  8패  GB 2.5  (승률  8/16 = 0.500)
 *
 *   승차 공식: GB = ((1위승 - 팀승) + (팀패 - 1위패)) / 2
 *     LG GB = ((10-10) + (6-5)) / 2 = 0.5
 *     KT GB = ((10- 8) + (8-5)) / 2 = 2.5
 */
@SpringBootTest
@Slf4j
class VerificationTest {

    // ── Real beans ──
    @Autowired StandingsAdjuster standingsAdjuster;
    @Autowired GameEndNotificationBuilder notificationBuilder;
    @Autowired GameProcessor gameProcessor;
    @Autowired MemberRepository memberRepository;

    // ── Mocked infra ──
    @MockBean NaverStandingsClient standingsClient;
    @MockBean NaverSportsClient naverSportsClient;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean TelegramNotificationClient telegramNotificationClient;

    // ── 공통 테스트 픽스처 ──
    private static final KboStandingsResult STALE_STANDINGS = new KboStandingsResult(
            "REGULAR_SEASON",
            List.of(
                    new KboTeamStandingDto("SS", "삼성", 1, 10, 0, 5, 0.0),
                    new KboTeamStandingDto("LG", "LG",  2,  9, 0, 6, 0.5),
                    new KboTeamStandingDto("KT", "KT",  3,  8, 0, 7, 1.0)
            )
    );

    private static final RealtimeGameInfoDto GAME_RESULT = RealtimeGameInfoDto.builder()
            .gameId("20260319KTLG02026")
            .awayTeamCode("LG")
            .homeTeamCode("KT")
            .awayTeamName("LG")
            .homeTeamName("KT")
            .awayScore(5)
            .homeScore(3)
            .statusCode("ENDED")
            .stadium("수원KT위즈파크")
            .isCanceled(false)
            .build();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // Redis 체이닝 모킹 (opsForValue → null 방지)
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(valueOps.get(anyString())).thenReturn(null);
        when(telegramNotificationClient.sendMessage(any())).thenReturn(TelegramSendResult.success(200));

        memberRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Test 1: StandingsAdjuster 직접 검증 — 보정 전후 메시지 상세 비교
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[직접 검증] StandingsAdjuster 보정 전후 순위/승차/메시지 비교")
    void 순위보정_전후_메시지_상세_비교() {
        // When: 경기 결과를 stale 순위표에 적용
        KboStandingsResult corrected = standingsAdjuster.applyGameResult(STALE_STANDINGS, GAME_RESULT);

        // LG 팬 / KT 팬 각각 보정 전후 메시지 생성
        String lgBefore = notificationBuilder.build("LG", GAME_RESULT, STALE_STANDINGS, null, false);
        String lgAfter  = notificationBuilder.build("LG", GAME_RESULT, corrected, null, false);
        String ktBefore = notificationBuilder.build("KT", GAME_RESULT, STALE_STANDINGS, null, false);
        String ktAfter  = notificationBuilder.build("KT", GAME_RESULT, corrected, null, false);

        // ── 보정된 순위표 수치 로그 ──
        log.info("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  [순위 보정 결과]");
        log.info("  보정 전 → 보정 후");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        STALE_STANDINGS.standings().forEach(before -> {
            KboTeamStandingDto after = corrected.standings().stream()
                    .filter(t -> t.teamCode().equals(before.teamCode()))
                    .findFirst().orElseThrow();
            boolean changed = before.wins() != after.wins()
                    || before.losses() != after.losses()
                    || before.gameBehind() != after.gameBehind()
                    || before.rank() != after.rank();
            log.info("  {} {}위 {}승{}무{}패 GB={} → {}위 {}승{}무{}패 GB={}  {}",
                    String.format("%-6s", before.teamName()),
                    before.rank(), before.wins(), before.draws(), before.losses(), before.gameBehind(),
                    after.rank(),  after.wins(),  after.draws(),  after.losses(),  after.gameBehind(),
                    changed ? "← 변경" : "(불변)");
        });

        // ── 메시지 Before/After 로그 ──
        log.info("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  [LG 팬 메시지] 보정 전 (stale API data)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n{}", lgBefore);
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  [LG 팬 메시지] 보정 후 (금일 경기 결과 반영)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n{}", lgAfter);
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  [KT 팬 메시지] 보정 전 (stale API data)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n{}", ktBefore);
        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  [KT 팬 메시지] 보정 후 (금일 경기 결과 반영)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n{}", ktAfter);

        // ── Assertions: 승패 수 ──
        assertThat(lgBefore).as("보정 전 LG stale 9승").contains("9승 0무 6패");
        assertThat(lgAfter).as("보정 후 LG 10승 반영").contains("10승 0무 6패");
        assertThat(ktBefore).as("보정 전 KT stale 7패").contains("8승 0무 7패");
        assertThat(ktAfter).as("보정 후 KT 8패 반영").contains("8승 0무 8패");

        // ── Assertions: 승차 ──
        KboTeamStandingDto lg = find(corrected, "LG");
        KboTeamStandingDto kt = find(corrected, "KT");
        assertThat(lg.gameBehind()).as("LG GB = 0.5").isEqualTo(0.5);
        assertThat(kt.gameBehind()).as("KT GB = 2.5").isEqualTo(2.5);

        // ── Assertions: 순위 불변 (LG는 여전히 2위, SS 1위) ──
        assertThat(find(corrected, "SS").rank()).isEqualTo(1);
        assertThat(lg.rank()).isEqualTo(2);
        assertThat(kt.rank()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Test 2: GameProcessor E2E — 보정된 순위가 실제 발송 메시지에 포함되는지
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[E2E] GameProcessor.process() → StandingsAdjuster 보정 → 텔레그램 발송 메시지 검증")
    void GameProcessor_경기종료_보정순위_텔레그램_메시지_검증() {
        // Given: LG 팬 테스트 멤버 DB 저장 (실제 텔레그램 발송 대상)
        memberRepository.save(Member.builder()
                .name("테스트팬")
                .supportTeam(Team.LG)
                .telegramId("TEST_CHAT_ID_LG")
                .notifyRealTimeAlert(true)
                .build());

        // Given: NaverStandingsClient → stale 순위 반환 (경기 결과 미반영)
        when(standingsClient.fetchStandings()).thenReturn(STALE_STANDINGS);

        // Given: NaverSportsClient → 경기 종료 상태 반환
        when(naverSportsClient.fetchGameInfo(GAME_RESULT.getGameId())).thenReturn(GAME_RESULT);
        when(naverSportsClient.fetchScheduledGames(any(), any())).thenReturn(List.of());

        ScheduledGameDto schedule = ScheduledGameDto.builder()
                .gameId(GAME_RESULT.getGameId())
                .awayTeamCode("LG")
                .homeTeamCode("KT")
                .awayTeamName("LG")
                .homeTeamName("KT")
                .stadium("수원KT위즈파크")
                .gameDateTime(LocalDateTime.now())
                .build();

        // When: GameProcessor.process() 호출
        gameProcessor.process(schedule, List.of());

        // Then: 발송된 메시지 캡처
        ArgumentCaptor<TelegramMessage> captor = ArgumentCaptor.forClass(TelegramMessage.class);
        verify(telegramNotificationClient, atLeastOnce()).sendMessage(captor.capture());

        List<TelegramMessage> sent = captor.getAllValues();

        log.info("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  [E2E] 실제 발송된 텔레그램 메시지 ({} 건)", sent.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sent.forEach(msg ->
                log.info("\nchatId={}\n{}", msg.getChatId(), msg.getText()));

        // LG 팬에게 전송된 메시지 추출
        TelegramMessage lgMsg = sent.stream()
                .filter(m -> "TEST_CHAT_ID_LG".equals(m.getChatId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("LG 팬 메시지가 발송되지 않았습니다"));

        // 보정된 순위 포함 여부 검증
        assertThat(lgMsg.getText())
                .as("보정된 LG 10승이 메시지에 포함되어야 함")
                .contains("10승");
        assertThat(lgMsg.getText())
                .as("stale LG 9승은 메시지에 없어야 함")
                .doesNotContain("9승 0무 6패");
    }

    private KboTeamStandingDto find(KboStandingsResult result, String teamCode) {
        return result.standings().stream()
                .filter(t -> teamCode.equals(t.teamCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("팀 코드 없음: " + teamCode));
    }
}
