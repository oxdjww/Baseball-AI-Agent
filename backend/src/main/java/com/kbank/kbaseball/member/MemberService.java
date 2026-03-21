package com.kbank.kbaseball.member;

import com.kbank.kbaseball.domain.team.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public List<Member> findAllSorted() {
        return memberRepository.findAll().stream()
                .sorted(Comparator.comparing(Member::getSeqId))
                .toList();
    }

    public Member findByIdOrThrow(Long id) {
        return memberRepository.findByLongId(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
    }

    @Transactional
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepository.deleteByLongId(id);
    }

    public List<Member> findByNotifyGameAnalysisTrue() {
        return memberRepository.findByNotifyGameAnalysisTrue();
    }

    public List<Member> findBySupportTeamAndNotifyRainAlertTrue(Team team) {
        return memberRepository.findBySupportTeamAndNotifyRainAlertTrue(team);
    }

}
