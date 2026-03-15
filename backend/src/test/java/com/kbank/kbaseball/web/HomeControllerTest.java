package com.kbank.kbaseball.web;

import com.kbank.kbaseball.auth.PendingMemberData;
import com.kbank.kbaseball.auth.TelegramLinkService;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.member.MemberRepository;
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

    @MockBean
    TelegramLinkService telegramLinkService;

    @Test
    void signup_validForm_redirectsToSignupSuccess() throws Exception {
        when(telegramLinkService.storePendingSignup(any())).thenReturn("testtoken1234567");

        mockMvc.perform(post("/home/signup")
                        .param("name", "테스트")
                        .param("supportTeam", "LG")
                        .param("notifyGameAnalysis", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup_success"));
    }

    @Test
    void signup_setsSessionSignupToken() throws Exception {
        when(telegramLinkService.storePendingSignup(any())).thenReturn("abc1234567890123");

        mockMvc.perform(post("/home/signup")
                        .param("name", "테스트")
                        .param("supportTeam", "LG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(request().sessionAttribute("signupToken", "abc1234567890123"));
    }

    @Test
    void signup_storesPendingWithCorrectFields() throws Exception {
        when(telegramLinkService.storePendingSignup(any())).thenReturn("token");

        mockMvc.perform(post("/home/signup")
                        .param("name", "테스트")
                        .param("supportTeam", "LG")
                        .param("notifyGameAnalysis", "true"))
                .andExpect(status().is3xxRedirection());

        verify(telegramLinkService).storePendingSignup(argThat((PendingMemberData p) ->
                "테스트".equals(p.name()) &&
                Team.LG == p.supportTeam() &&
                p.notifyGameAnalysis()
        ));
    }

    @Test
    void signup_withAllNotificationsTrue_storesCorrectly() throws Exception {
        when(telegramLinkService.storePendingSignup(any())).thenReturn("token");

        mockMvc.perform(post("/home/signup")
                        .param("name", "알림왕")
                        .param("supportTeam", "LT")
                        .param("notifyGameAnalysis", "true")
                        .param("notifyRainAlert", "true")
                        .param("notifyRealTimeAlert", "true"))
                .andExpect(status().is3xxRedirection());

        verify(telegramLinkService).storePendingSignup(argThat((PendingMemberData p) ->
                p.notifyGameAnalysis() &&
                p.notifyRainAlert() &&
                p.notifyRealTimeAlert()
        ));
    }

    @Test
    void signup_withNoNotifications_storesWithDefaultFalse() throws Exception {
        when(telegramLinkService.storePendingSignup(any())).thenReturn("token");

        mockMvc.perform(post("/home/signup")
                        .param("name", "조용이")
                        .param("supportTeam", "HH"))
                .andExpect(status().is3xxRedirection());

        verify(telegramLinkService).storePendingSignup(argThat((PendingMemberData p) ->
                !p.notifyGameAnalysis() &&
                !p.notifyRainAlert() &&
                !p.notifyRealTimeAlert()
        ));
    }
}
