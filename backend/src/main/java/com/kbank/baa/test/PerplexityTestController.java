package com.kbank.baa.test;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PerplexityTestController {

    private final OpenAiChatModel chatModel;

    @GetMapping("/test/ai")
    public Map<String, Object> testPerplexity(@RequestParam(value = "message", defaultValue = "Hello, Perplexity!") String message) {
        return Map.of("response", chatModel.call(message));
    }
}
