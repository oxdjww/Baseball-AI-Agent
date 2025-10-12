package com.kbank.baa.member;


import com.kbank.baa.admin.Team;
import com.kbank.baa.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Team supportTeam;

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
