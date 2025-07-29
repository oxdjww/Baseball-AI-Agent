// src/main/java/com/kbank/baa/admin/Team.java
package com.kbank.baa.admin;

import lombok.Getter;

@Getter
public enum Team {
    HH("한화", 156),   // 대전
    LG("LG", 109),    // 서울
    OB("두산", 109),  // 서울
    WO("키움", 109),  // 서울 (스카이돔 인근 지점번호가 없는 관계로 서울 사용)
    LT("롯데", 159),  // 부산
    HT("KIA", 156),   // 광주
    KT("KT", 119),    // 수원
    SK("SSG", 112),   // 인천
    NC("NC", 218),    // 창원
    SS("삼성", 224);   // 대구

    private final String displayName;
    private final int    stn;  // 기상청 지점번호

    Team(String displayName, int stn) {
        this.displayName = displayName;
        this.stn = stn;
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
