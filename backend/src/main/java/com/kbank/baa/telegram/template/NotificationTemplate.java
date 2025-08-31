package com.kbank.baa.telegram.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationTemplate {
    // 공지/알림
    ANNOUNCEMENT("<b>새로운 공지사항이 있어요!</b>\n\n%s\n\n감사합니다."),
    // 단순 종료 알림(스코어 없음)
    GAME_ENDED("금일 %s의 경기가 종료되었습니다.\n\n⚾️ 1시간 뒤, AI 게임 분석 레포트가 전송됩니다!\n\n감사합니다."),
    // 스코어 포함 종료 알림
    GAME_ENDED_WITH_SCORE(
            "📢 금일 %s의 경기가 종료되었습니다.\n" +
                    "🏟️ 최종 스코어: %s %d : %d %s\n\n" +
                    "⚾️ 1시간 뒤, AI 게임 분석 레포트가 전송됩니다!\n\n감사합니다."
    ),
    // 우천취소 알림
    GAME_CANCELED(
            "☔ 금일 %s의 경기가 우천으로 취소되었습니다.\n" +
                    "상대: %s\n\n" +
                    "다음 경기를 기대해 주세요."
    ),
    // 웹훅/연동 플로우
    LINK_SUCCESS("텔레그램 연동이 완료되었습니다 ✅"),
    ACCOUNT_NOT_FOUND("계정을 찾을 수 없습니다. 웹에서 다시 시도해주세요."),
    TOKEN_EXPIRED("토큰이 만료되었어요. 웹에서 다시 시도해주세요."),
    WELCOME_GUIDE("안녕하세요!\n🧢 Baseball AI Agent입니다.\n\n매일 진행되는 야구 경기 알림을 받아보세요! ⚾️\n기능 오작동 및 오류 문의는 아래 링크로 제보해 주세요.\n\n%s\n\n감사합니다."),

    // 범용
    GENERIC("%s");

    private final String pattern;

    public String format(Object... args) {
        return String.format(pattern, args);
    }
}
