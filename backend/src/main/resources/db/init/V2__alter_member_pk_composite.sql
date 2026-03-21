-- member 테이블 PK를 단일(id) → 복합(id, telegram_id)으로 변경
-- 운영 DB에는 이미 적용된 상태. dev/staging 환경 동기화용.

-- 주의: NULL telegram_id 행이 존재하면 아래 구문 실행 전 정리 필요
-- DELETE FROM member WHERE telegram_id IS NULL;

ALTER TABLE member
    ALTER COLUMN telegram_id SET NOT NULL;

ALTER TABLE member
    DROP CONSTRAINT member_pkey;

ALTER TABLE member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id, telegram_id);
