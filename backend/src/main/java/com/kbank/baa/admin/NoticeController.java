package com.kbank.baa.admin;

import com.kbank.baa.telegram.TelegramService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/telegram/notice")
public class NoticeController {

    private final TelegramService telegramService;

    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> sendNoticeToAllMembers(
            @RequestBody NoticeRequest request
    ) {
        String message = request.getMessage();
        telegramService.sendAnnouncementToAllMembers(message);
        return ResponseEntity.ok(Map.of(
                "sentMessage", message
        ));
    }

    @Data
    @AllArgsConstructor
    static class NoticeRequest {
        private String message;
    }
}
