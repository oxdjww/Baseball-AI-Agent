package com.kbank.baa.auth;

import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramLinkService {

    private static final String BOT_USERNAME = "baseball_ai_agent_bot";
    private static final String REDIS_KEY_PREFIX = "tg:link:";

    private final StringRedisTemplate redis;
    private final MemberService memberService;

    public String generateLinkUrl(Long memberId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        redis.opsForValue().set(REDIS_KEY_PREFIX + token, String.valueOf(memberId), Duration.ofMinutes(5));
        return "https://t.me/" + BOT_USERNAME + "?start=" + token;
    }

    @Transactional
    public LinkResult linkAccount(String token, Long telegramUserId) {
        String key = REDIS_KEY_PREFIX + token;
        String memberIdStr = redis.opsForValue().get(key);

        log.info("[TelegramLinkService][linkAccount] token={} -> memberId={}", token, memberIdStr);

        if (memberIdStr == null) {
            return new LinkResult.TokenExpired();
        }

        Long memberId = Long.valueOf(memberIdStr);
        Member member;
        try {
            member = memberService.findByIdOrThrow(memberId);
        } catch (IllegalArgumentException e) {
            return new LinkResult.MemberNotFound();
        }

        memberService.linkTelegramId(memberId, String.valueOf(telegramUserId));

        // Redis 삭제는 DB 커밋 성공 후 실행 — 트랜잭션 롤백 시 Redis 불일치 방지
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redis.delete(key);
                log.info("[TelegramLinkService][linkAccount] Redis token deleted after commit: {}", key);
            }
        });

        return new LinkResult.Linked(member.getName());
    }
}
