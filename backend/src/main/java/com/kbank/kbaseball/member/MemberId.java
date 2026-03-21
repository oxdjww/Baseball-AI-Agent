package com.kbank.kbaseball.member;

import java.io.Serializable;
import java.util.Objects;

public class MemberId implements Serializable {

    private Long id;
    private String telegramId;

    public MemberId() {}

    public MemberId(Long id, String telegramId) {
        this.id = id;
        this.telegramId = telegramId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberId other)) return false;
        return Objects.equals(id, other.id) &&
               Objects.equals(telegramId, other.telegramId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, telegramId);
    }
}
