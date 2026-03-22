// src/main/java/com/kbank/kbaseball/domain/team/Team.java
package com.kbank.kbaseball.domain.team;

import lombok.Getter;

@Getter
public enum Team {
    HH("한화", 156, "#FF6B00"),   // 대전
    LG("LG", 109,  "#C30452"),    // 서울
    OB("두산", 109, "#131230"),   // 서울
    WO("키움", 109, "#820024"),   // 서울
    LT("롯데", 159, "#E6222B"),   // 부산
    HT("KIA", 156,  "#EA0029"),   // 광주
    KT("KT", 119,   "#000000"),   // 수원
    SK("SSG", 112,  "#CE0E2D"),   // 인천
    NC("NC", 218,   "#315288"),   // 창원
    SS("삼성", 224, "#1428A0");   // 대구

    private final String displayName;
    private final int stn;  // 기상청 지점번호
    private final String primaryColor;  // 팀 상징 컬러

    Team(String displayName, int stn, String primaryColor) {
        this.displayName = displayName;
        this.stn = stn;
        this.primaryColor = primaryColor;
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
}
