// src/main/java/com/kbank/kbaseball/domain/team/Team.java
package com.kbank.kbaseball.domain.team;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum Team {
    HH("한화", 133, "#FF6B00", "한화"),
    LG("LG",   109, "#C30452", "LG"),
    OB("두산", 109, "#131230", "두산"),
    WO("키움", 109, "#820024", "키움"),
    LT("롯데", 159, "#E6222B", "롯데"),
    HT("KIA",  156, "#EA0029", "KIA"),
    KT("KT",   119, "#000000", "KT"),
    SK("SSG",  112, "#CE0E2D", "SSG"),
    NC("NC",   218, "#315288", "NC"),
    SS("삼성", 143, "#1428A0", "삼성");

    private final String displayName;
    private final int stn;           // 기상청 지점번호
    private final String primaryColor;
    private final String naverTeamName; // Naver 통계 API teamName 필드값

    Team(String displayName, int stn, String primaryColor, String naverTeamName) {
        this.displayName = displayName;
        this.stn = stn;
        this.primaryColor = primaryColor;
        this.naverTeamName = naverTeamName;
    }

    public static Team of(String code) {
        try {
            return Team.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 팀 코드: " + code);
        }
    }

    public static String getDisplayNameByCode(String code) {
        return of(code).getDisplayName();
    }

    /** Naver 통계 API의 teamName("LG트윈스" 등)을 내부 팀 코드("LG")로 변환 */
    public static String fromNaverTeamName(String naverTeamName) {
        for (Team t : values()) {
            if (t.naverTeamName.equals(naverTeamName)) return t.name();
        }
        log.warn("[Team] Naver teamName 매핑 없음: '{}' — 원본 반환", naverTeamName);
        return naverTeamName;
    }
}
