package com.kbank.baa.sports;

import org.springframework.stereotype.Component;

@Component
public class GameMessageFormatter {

    /**
     * GameInfo → "9회말에 키움 vs 삼성, 0:1" 형태의 문장 생성
     */
    public String format(GameInfo info) {
        return String.format("%s에 %s vs %s, %d:%d",
                info.getInning(),
                info.getHomeTeam(),
                info.getAwayTeam(),
                info.getHomeScore(),
                info.getAwayScore());
    }
}
