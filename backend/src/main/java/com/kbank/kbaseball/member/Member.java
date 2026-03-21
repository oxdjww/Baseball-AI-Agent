package com.kbank.kbaseball.member;


import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(MemberId.class)
public class Member extends BaseEntity implements Persistable<MemberId> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_id_seq", allocationSize = 1)
    @Getter(AccessLevel.NONE)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Team supportTeam;

    @Id
    @Column(name = "telegram_id", nullable = false)
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

    /**
     * Persistable 계약: 복합키 반환
     * id == null 이면 새 엔티티로 판단하여 persist() 실행 (SELECT 방지)
     */
    @Override
    public MemberId getId() {
        return new MemberId(this.id, this.telegramId);
    }

    /**
     * Long 타입 시퀀스 id 반환 (세션, URL, 정렬 등 기존 호출부 전용)
     */
    public Long getSeqId() {
        return this.id;
    }

    @Transient
    @Override
    public boolean isNew() {
        return this.id == null;
    }
}
