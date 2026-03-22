-- telegram_id UNIQUE 제약 → partial unique index 로 교체
-- 탈퇴(deleted=true) 회원의 telegram_id 는 unique 검사 제외
-- → 탈퇴 후 동일 telegram_id 로 재가입 가능
ALTER TABLE member DROP CONSTRAINT member_telegram_id_unique;

CREATE UNIQUE INDEX member_telegram_id_active_unique
    ON member(telegram_id)
    WHERE deleted = false;
