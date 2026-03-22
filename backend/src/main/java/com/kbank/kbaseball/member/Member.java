package com.kbank.kbaseball.member;


import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Team supportTeam;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private String telegramId;

    @Builder.Default
    private boolean notifyGameAnalysis = false;

    @Builder.Default
    private boolean notifyRainAlert = false;

    @Builder.Default
    private boolean notifyRealTimeAlert = false;

    @Builder.Default
    private boolean deleted = false;
}
