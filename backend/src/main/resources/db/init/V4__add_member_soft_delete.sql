-- 회원 소프트 삭제 컬럼 추가
-- 기존 회원은 deleted = false (DEFAULT) 로 자동 설정
ALTER TABLE member ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
