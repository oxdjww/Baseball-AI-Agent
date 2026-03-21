-- 복합 PK(id, telegram_id) → 단일 PK(id) 복원 + telegram_id UNIQUE 제약 추가
-- 주의: 중복 telegram_id가 있으면 UNIQUE 제약 추가 실패.
-- 실행 전 확인: SELECT telegram_id, COUNT(*) FROM member GROUP BY telegram_id HAVING COUNT(*) > 1;

ALTER TABLE member DROP CONSTRAINT member_pkey;

ALTER TABLE member ADD CONSTRAINT member_pkey PRIMARY KEY (id);

ALTER TABLE member ADD CONSTRAINT member_telegram_id_unique UNIQUE (telegram_id);
