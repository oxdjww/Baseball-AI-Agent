# DB 스키마 진화 — Flyway V1~V5

## 개요

운영 데이터베이스에 적용된 마이그레이션 5버전의 변경 내역과 각 결정의 근거를 기록한다.
스키마 변경은 SQL 문제가 아니라 "운영 데이터를 어떻게 다룰 것인가"의 판단 문제다.

---

## V1 — Feature Toggle 테이블 (`system_settings`)

```sql
CREATE TABLE IF NOT EXISTS system_settings (
    feature_key VARCHAR(100) PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO system_settings (feature_key, enabled) VALUES
    ('AI_ANALYSIS',        FALSE),
    ('REVERSAL_DETECTION', TRUE),
    ('RAIN_ALERT',         TRUE)
ON CONFLICT (feature_key) DO NOTHING;
```

**결정 이유**

기능별 on/off를 코드 배포 없이 런타임에 제어하기 위해 DB 기반 Feature Toggle을 도입했다.
AI 분석처럼 외부 API 비용이 발생하는 기능을 즉시 차단할 수 있어야 했다.
`ON CONFLICT DO NOTHING`으로 멱등성을 보장해 재시작이나 마이그레이션 재실행 시 데이터가 중복되지 않는다.

---

## V2 — 복합 PK 도입 (실수)

```sql
-- member 테이블 PK를 단일(id) → 복합(id, telegram_id)으로 변경
ALTER TABLE member ALTER COLUMN telegram_id SET NOT NULL;
ALTER TABLE member DROP CONSTRAINT member_pkey;
ALTER TABLE member ADD CONSTRAINT member_pkey PRIMARY KEY (id, telegram_id);
```

**의도:** telegram_id를 PK에 포함시켜 DB 수준에서 UNIQUE를 보장하려 했다.

**문제:** 운영 DB에 `telegram_id = NULL`인 기존 회원 데이터가 존재했다.
`SET NOT NULL` 구문이 기존 행에 적용되면서 오류가 발생했고,
`telegramId`가 없는 상태에서의 Member 생성 흐름이 전부 깨졌다.
복합 PK는 JPA 엔티티 매핑도 복잡하게 만들어 이점보다 비용이 컸다.

---

## V3 — 단일 PK 복원 + UNIQUE 제약 분리

```sql
-- 실행 전 확인: SELECT telegram_id, COUNT(*) FROM member GROUP BY telegram_id HAVING COUNT(*) > 1;
ALTER TABLE member DROP CONSTRAINT member_pkey;
ALTER TABLE member ADD CONSTRAINT member_pkey PRIMARY KEY (id);
ALTER TABLE member ADD CONSTRAINT member_telegram_id_unique UNIQUE (telegram_id);
```

**결정:** PK는 단일 `id`로 복원하고, telegram_id UNIQUE를 별도 제약으로 분리했다.
PK는 식별자 역할에만 집중하고, 비즈니스 제약(telegram_id 중복 방지)은 별도 레이어로 다루는 것이 맞다.

실행 전 중복 telegram_id 확인 쿼리를 SQL 파일 주석에 명시했다.
제약 추가 전 데이터 정합성 검증을 생략하면 마이그레이션 자체가 실패하기 때문이다.

---

## V4 — Soft Delete (`deleted` 컬럼)

```sql
ALTER TABLE member ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
```

**결정 이유**

회원 탈퇴 시 즉시 DELETE 대신 `deleted = true`로 표시하는 Soft Delete를 택했다.

- 동일 Telegram 계정으로 재가입 시 이전 설정 히스토리 복원 가능성 확보
- 탈퇴 직후 발생할 수 있는 미처리 알림 발송 중 외래 키 오류 방지
- 감사(audit) 목적: 탈퇴 회원 수, 탈퇴 시점 추적

`DEFAULT FALSE`로 기존 회원을 활성 상태로 유지하므로 데이터 마이그레이션이 불필요하다.

조회 쿼리에서 `deleted = false` 조건을 항상 포함해야 하며, MemberRepository의 커스텀 쿼리에 적용되어 있다.

---

## V5 — Partial Unique Index 전환

```sql
-- 기존 UNIQUE 제약 제거
ALTER TABLE member DROP CONSTRAINT member_telegram_id_unique;

-- 활성 회원(deleted=false)에 대해서만 telegram_id UNIQUE 보장
CREATE UNIQUE INDEX member_telegram_id_active_unique
    ON member(telegram_id)
    WHERE deleted = false;
```

**V3 UNIQUE 제약의 문제**

V3에서 추가한 `member_telegram_id_unique`는 `deleted = true` 행도 포함한다.
탈퇴한 회원의 telegram_id가 UNIQUE 제약에 걸려 동일 Telegram 계정으로 재가입이 불가능했다.

**Partial Unique Index 선택 이유**

PostgreSQL의 `WHERE` 절 있는 UNIQUE 인덱스는 조건에 해당하는 행에만 UNIQUE를 적용한다.
탈퇴 회원(`deleted = true`)은 인덱스 대상에서 제외되므로 재가입이 허용된다.
활성 회원 중 동일 telegram_id 중복은 여전히 DB 수준에서 방지된다.

**PostgreSQL 인덱스 주의사항**

PostgreSQL은 FK 컬럼에 인덱스를 자동 생성하지 않는다.
이 partial unique index는 telegram_id 조회 성능도 함께 개선한다 (`existsByTelegramId` 쿼리).

---

## 마이그레이션 히스토리 요약

| 버전 | 변경 내용 | 핵심 결정 |
|---|---|---|
| V1 | `system_settings` 테이블 생성 | DB 기반 Feature Toggle, 멱등성 보장 |
| V2 | 복합 PK 도입 | **실수** — NULL 데이터 충돌, 복원 필요 |
| V3 | 단일 PK 복원 + UNIQUE 제약 분리 | PK와 비즈니스 제약 분리 원칙 |
| V4 | `deleted` 컬럼 추가 (Soft Delete) | 즉시 삭제 대신 논리 삭제 |
| V5 | Partial Unique Index 전환 | 탈퇴 회원 telegram_id 재사용 허용 |

---

## 관련 파일

| 파일 | 역할 |
|---|---|
| `db/init/V1__create_system_settings.sql` | Feature Toggle 초기화 |
| `db/init/V2__alter_member_pk_composite.sql` | 복합 PK 시도 (실패) |
| `db/init/V3__fix_member_pk_and_unique_telegram_id.sql` | PK 복원 + UNIQUE 분리 |
| `db/init/V4__add_member_soft_delete.sql` | Soft Delete 컬럼 |
| `db/init/V5__partial_unique_telegram_id.sql` | Partial Unique Index |
| `member/MemberRepository.java` | `deleted = false` 조건 포함 쿼리 |
