package com.kbank.baa.admin;

import lombok.Getter;

@Getter
public enum Team {
    HH("한화"),
    LG("LG"),
    LT("롯데"),
    HT("KIA"),
    KT("KT"),
    SK("SSG"),
    NC("NC"),
    SS("삼성"),
    OB("두산"),
    WO("키움");

    private final String displayName;

    Team(String displayName) {
        this.displayName = displayName;
    }
}
