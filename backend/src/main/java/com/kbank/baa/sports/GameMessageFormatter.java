package com.kbank.baa.sports;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.baa.member.Member;
import com.kbank.baa.admin.Team;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import org.springframework.stereotype.Component;

@Component
public class GameMessageFormatter {

    public String format(Member member, RealtimeGameInfoDto g) {
        return String.format(
                "%s님의 응원팀 %s의 경기 현황을 알려드려요.%n" +
                        "[%s vs %s] %s %s 경기 %n" +
                        "%s | %d:%d",
                member.getName(),
                member.getSupportTeam().name(),
                g.getHomeTeamName(),
                g.getAwayTeamName(),
                g.getStadium(),
                g.getGameDateTime().toLocalTime(),
                g.getInning(),
                g.getHomeScore(),
                g.getAwayScore()
        );
    }

    public String formatLeadChange(Member m,
                                   RealtimeGameInfoDto info,
                                   String prevLeader,
                                   String currLeader) {
        int awayTeamScore = info.getAwayScore();
        int homeTeamScore = info.getHomeScore();

        String awayTeamName = info.getAwayTeamName();
        String homeTeamName = info.getHomeTeamName();

        String currInning = info.getInning();

        String memberSupportTeamName = m.getSupportTeam().getDisplayName();

        if ("NONE".equals(currLeader)) {
            // 동점 상황
            return String.format(
                    "[<b>%s</b> VS <b>%s</b>] 경기 상황에 변동이 있어요!\n" +
                            "%s, 경기가 <b>%d : %d</b> 동점이 되었습니다! 🔥",
                    awayTeamName,
                    homeTeamName,
                    currInning,
                    awayTeamScore,
                    homeTeamScore
            );
        } else {
            String leadingTeamName = Team.getDisplayNameByCode(currLeader);

            if (memberSupportTeamName.equals(leadingTeamName)) {
                // 내가 응원하는 팀이 역전했을 때
                return String.format(
                        "[<b>%s</b> VS <b>%s</b>] 짜릿한 순간! 🎉\n" +
                                "응원하는 <b>%s팀</b>이 앞서 나갑니다!\n" +
                                "%s, 현재 스코어는 <b>%d:%d</b> 🔥",
                        awayTeamName,
                        homeTeamName,
                        leadingTeamName,
                        currInning,
                        awayTeamScore,
                        homeTeamScore
                );
            } else {
                // 상대 팀이 역전했을 때
                return String.format(
                        "[<b>%s</b> VS <b>%s</b>] 아쉽네요... 😥\n" +
                                "%s, <b>%s팀</b>이 <b>%d:%d</b>로 경기를 앞서 나갑니다.",
                        awayTeamName,
                        homeTeamName,
                        currInning,
                        leadingTeamName,
                        awayTeamScore,
                        homeTeamScore
                );
            }
        }
    }

    public String formatScoreboard(JsonNode scoreBoard) {
        final int TEAM_COL = 4;   // "AWAY" / "HOME" 길이
        final int TEAM_GAP = 2;   // 팀라벨-이닝 사이 간격
        final int INN_W = 2;    // 이닝/점수 폭 (두 자리 대응)
        final int INN_GAP = 1;    // 이닝 칸 사이 공백
        final int SUM_W = 2;    // R/H/E/B 폭
        final int SUM_GAP = 2;    // 이닝 블록 ↔ 합계 블록 간 공백
        final String NP = "-";  // 안 친 이닝

        JsonNode rA = scoreBoard.path("rheb").path("away");
        JsonNode rH = scoreBoard.path("rheb").path("home");
        JsonNode iA = scoreBoard.path("inn").path("away");
        JsonNode iH = scoreBoard.path("inn").path("home");

        int innA = iA.isArray() ? iA.size() : 0;
        int innH = iH.isArray() ? iH.size() : 0;
        int INNINGS = Math.max(9, Math.max(innA, innH)); // 연장 포함

        StringBuilder raw = new StringBuilder();

        // 헤더
        raw.append(padRightChars("", TEAM_COL)).append(repeat(' ', TEAM_GAP));
        for (int i = 1; i <= INNINGS; i++) {
            if (i > 1) raw.append(repeat(' ', INN_GAP));
            raw.append(padLeft(String.valueOf(i), INN_W));
        }
        raw.append(repeat(' ', SUM_GAP))
                .append(padLeft("R", SUM_W)).append(' ')
                .append(padLeft("H", SUM_W)).append(' ')
                .append(padLeft("E", SUM_W)).append(' ')
                .append(padLeft("B", SUM_W))
                .append('\n');

        // AWAY
        raw.append(padRightChars("AWAY", TEAM_COL)).append(repeat(' ', TEAM_GAP));
        for (int i = 0; i < INNINGS; i++) {
            if (i > 0) raw.append(repeat(' ', INN_GAP));
            String v = (iA.isArray() && i < iA.size()) ? iA.get(i).asText() : NP;
            raw.append(padLeft(v, INN_W));
        }
        raw.append(repeat(' ', SUM_GAP))
                .append(padLeft(String.valueOf(rA.path("r").asInt(0)), SUM_W)).append(' ')
                .append(padLeft(String.valueOf(rA.path("h").asInt(0)), SUM_W)).append(' ')
                .append(padLeft(String.valueOf(rA.path("e").asInt(0)), SUM_W)).append(' ')
                .append(padLeft(String.valueOf(rA.path("b").asInt(0)), SUM_W))
                .append('\n');

        // HOME
        raw.append(padRightChars("HOME", TEAM_COL)).append(repeat(' ', TEAM_GAP));
        for (int i = 0; i < INNINGS; i++) {
            if (i > 0) raw.append(repeat(' ', INN_GAP));
            String v = (iH.isArray() && i < iH.size()) ? iH.get(i).asText() : NP;
            raw.append(padLeft(v, INN_W));
        }
        raw.append(repeat(' ', SUM_GAP))
                .append(padLeft(String.valueOf(rH.path("r").asInt(0)), SUM_W)).append(' ')
                .append(padLeft(String.valueOf(rH.path("h").asInt(0)), SUM_W)).append(' ')
                .append(padLeft(String.valueOf(rH.path("e").asInt(0)), SUM_W)).append(' ')
                .append(padLeft(String.valueOf(rH.path("b").asInt(0)), SUM_W));

        // 텔레그램 HTML <pre>로 전송 (parse_mode=HTML 필수)
        return "<pre>" + escapeHtml(raw.toString()) + "</pre>";
    }


    // --- helpers (심플, 문자수 기준) ---
    private static String padLeft(String s, int w) {
        if (s == null) s = "";
        int pad = Math.max(0, w - s.length());
        return repeat(' ', pad) + s;
    }

    private static String padRightChars(String s, int w) {
        if (s == null) s = "";
        if (s.length() > w) s = s.substring(0, w);
        int pad = w - s.length();
        return s + repeat(' ', pad);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public String withParticle(String word, String particleWithBatchim, String particleWithoutBatchim) {
        if (word == null || word.isEmpty()) return word;

        char lastChar = word.charAt(word.length() - 1);

        // 한글이 아니면 기본적으로 받침 없는 조사 적용
        if (lastChar < 0xAC00 || lastChar > 0xD7A3) {
            return word + particleWithoutBatchim;
        }

        int baseCode = lastChar - 0xAC00;
        int jongseong = baseCode % 28;

        if (jongseong == 0) { // 받침 없음
            return word + particleWithoutBatchim;
        } else { // 받침 있음
            return word + particleWithBatchim;
        }
    }
}
