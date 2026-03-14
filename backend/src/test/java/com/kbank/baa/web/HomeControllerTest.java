package com.kbank.baa.web;

import com.kbank.baa.domain.team.Team;
import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HomeController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class HomeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MemberRepository memberRepository;

    @Test
    void signup_validForm_redirectsToSignupSuccess() throws Exception {
        Member saved = Member.builder().id(1L).name("테스트").supportTeam(Team.LG).build();
        when(memberRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/home/signup")
                        .param("name", "테스트")
                        .param("supportTeam", "LG")
                        .param("notifyGameAnalysis", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup_success"));
    }

    @Test
    void signup_savesMemberWithCorrectFields() throws Exception {
        Member saved = Member.builder().id(1L).name("테스트").supportTeam(Team.LG).build();
        when(memberRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/home/signup")
                        .param("name", "테스트")
                        .param("supportTeam", "LG")
                        .param("notifyGameAnalysis", "true"))
                .andExpect(status().is3xxRedirection());

        verify(memberRepository).save(argThat(m ->
                "테스트".equals(m.getName()) &&
                Team.LG == m.getSupportTeam() &&
                m.isNotifyGameAnalysis()
        ));
    }

    @Test
    void signup_setsSessionMemberId() throws Exception {
        Member saved = Member.builder().id(42L).name("테스트").supportTeam(Team.LG).build();
        when(memberRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/home/signup")
                        .param("name", "테스트")
                        .param("supportTeam", "LG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(request().sessionAttribute("memberId", 42L));
    }

    @Test
    void signup_withAllNotificationsTrue_savesCorrectly() throws Exception {
        Member saved = Member.builder().id(1L).name("알림왕").supportTeam(Team.LT).build();
        when(memberRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/home/signup")
                        .param("name", "알림왕")
                        .param("supportTeam", "LT")
                        .param("notifyGameAnalysis", "true")
                        .param("notifyRainAlert", "true")
                        .param("notifyRealTimeAlert", "true"))
                .andExpect(status().is3xxRedirection());

        verify(memberRepository).save(argThat(m ->
                m.isNotifyGameAnalysis() &&
                m.isNotifyRainAlert() &&
                m.isNotifyRealTimeAlert()
        ));
    }

    @Test
    void signup_withNoNotifications_savesWithDefaultFalse() throws Exception {
        Member saved = Member.builder().id(1L).name("조용이").supportTeam(Team.HH).build();
        when(memberRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/home/signup")
                        .param("name", "조용이")
                        .param("supportTeam", "HH"))
                .andExpect(status().is3xxRedirection());

        verify(memberRepository).save(argThat(m ->
                !m.isNotifyGameAnalysis() &&
                !m.isNotifyRainAlert() &&
                !m.isNotifyRealTimeAlert()
        ));
    }
}
