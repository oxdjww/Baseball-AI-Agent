// ScoreTracker.java
package com.kbank.baa.sports;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ScoreTracker {
    private static class Score {
        final int home, away;

        Score(int home, int away) {
            this.home = home;
            this.away = away;
        }
    }

    // key: gameId, value: 이전 스코어
    private final Map<String, Score> prevScores = new ConcurrentHashMap<>();

    /**
     * 이전 홈 점수 반환 (없으면 null)
     */
    public Integer getPrevHome(String gameId) {
        return prevScores.containsKey(gameId)
                ? prevScores.get(gameId).home
                : null;
    }

    /**
     * 이전 어웨이 점수 반환 (없으면 null)
     */
    public Integer getPrevAway(String gameId) {
        return prevScores.containsKey(gameId)
                ? prevScores.get(gameId).away
                : null;
    }

    /**
     * 현재 스코어를 저장합니다.
     *
     * @param gameId 경기 ID
     * @param home   홈 팀 점수
     * @param away   어웨이 팀 점수
     */
    public void update(String gameId, int home, int away) {
        log.debug("[ScoreTracker] {} 에 이전 스코어 저장 → {}:{}", gameId, home, away);
        prevScores.put(gameId, new Score(home, away));
    }
}
