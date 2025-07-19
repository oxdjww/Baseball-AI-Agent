package com.kbank.baa.admin;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String supportTeam;
    private String telegramId;

    /**
     * 경기 분석 알림
     */
    @Builder.Default
    private boolean notifyGameAnalysis = false;

    /**
     * 우천 알림
     */
    @Builder.Default
    private boolean notifyRainAlert = false;

    /**
     * 실시간 알림
     */
    @Builder.Default
    private boolean notifyRealTimeAlert = false;
}
