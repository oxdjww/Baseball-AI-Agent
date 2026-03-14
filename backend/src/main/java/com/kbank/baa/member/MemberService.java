package com.kbank.baa.member;

import com.kbank.baa.domain.team.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public List<Member> findAllSorted() {
        return memberRepository.findAll().stream()
                .sorted(Comparator.comparing(Member::getId))
                .toList();
    }

    public Member findByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
    }

    @Transactional
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepository.deleteById(id);
    }

    @Transactional
    public void linkTelegramId(Long memberId, String telegramId) {
        Member m = findByIdOrThrow(memberId);
        m.setTelegramId(telegramId);
        memberRepository.save(m);
    }

    public List<Member> findByNotifyGameAnalysisTrue() {
        return memberRepository.findByNotifyGameAnalysisTrue();
    }

    public List<Member> findBySupportTeamAndNotifyRainAlertTrue(Team team) {
        return memberRepository.findBySupportTeamAndNotifyRainAlertTrue(team);
    }

    @Transactional
    public int purgeOldPendingMembers(Instant before) {
        return memberRepository.deleteOldPending(before);
    }
}
