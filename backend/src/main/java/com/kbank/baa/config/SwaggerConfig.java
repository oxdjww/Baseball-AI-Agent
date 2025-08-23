package com.kbank.baa.config; // 패키지는 프로젝트 구조에 맞게 변경

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI baseballAiAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Baseball AI Agent API")
                        .description("""
                                KBO 실시간 알림/예측을 제공하는 Baseball AI Agent의 REST API 문서입니다.
                                - 역전 알림, 우천 취소 알림, 리드 체인지 감지 등
                                - 내부 배치/실시간 처리와의 연동 엔드포인트
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Baseball AI Agent Team")
                                .url("https://github.com/oxdjww/Baseball-AI-Agent")
                                .email("oxdjww@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local(Dev)"),
                        new Server().url("https://oxdjww.xyz").description("Production")
                ))
                .components(new Components());
    }
}
