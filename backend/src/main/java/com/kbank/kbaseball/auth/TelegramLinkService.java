package com.kbank.kbaseball.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberService;
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

    private static final String REDIS_KEY_PREFIX = "pending:signup:";

    private final StringRedisTemplate redis;
    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    public String storePendingSignup(PendingMemberData data) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String key = REDIS_KEY_PREFIX + token;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(data), Duration.ofMinutes(15));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("PendingMemberData 직렬화 실패", e);
        }
        log.info("[TelegramLinkService][storePendingSignup] token={} stored", token);
        return token;
    }

    public PendingMemberData getPendingMember(String token) {
        String json = redis.opsForValue().get(REDIS_KEY_PREFIX + token);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, PendingMemberData.class);
        } catch (JsonProcessingException e) {
            log.error("[TelegramLinkService][getPendingMember] 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public LinkResult linkAccount(String token, Long telegramUserId) {
        String key = REDIS_KEY_PREFIX + token;
        String json = redis.opsForValue().get(key);

        log.info("[TelegramLinkService][linkAccount] token={} -> json found={}", token, json != null);

        if (json == null) {
            return new LinkResult.TokenExpired();
        }

        PendingMemberData data;
        try {
            data = objectMapper.readValue(json, PendingMemberData.class);
        } catch (JsonProcessingException e) {
            log.error("[TelegramLinkService][linkAccount] 역직렬화 실패: {}", e.getMessage());
            return new LinkResult.TokenExpired();
        }

        Member saved = memberService.save(
                Member.builder()
                        .name(data.name())
                        .supportTeam(data.supportTeam())
                        .notifyGameAnalysis(data.notifyGameAnalysis())
                        .notifyRainAlert(data.notifyRainAlert())
                        .notifyRealTimeAlert(data.notifyRealTimeAlert())
                        .telegramId(String.valueOf(telegramUserId))
                        .build()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redis.delete(key);
                log.info("[TelegramLinkService][linkAccount] Redis token deleted after commit: {}", key);
            }
        });

        return new LinkResult.Linked(saved.getName());
    }
}
