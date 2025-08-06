package com.kbank.baa.admin;

import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/telegram/notice")
public class NoticeController {

    private final TelegramService telegramService;

    @PostMapping("/all")
    public ResponseEntity<?> sendNoticeToAllMembers(
            @RequestParam(value = "message") String message
    ) {
        telegramService.sendMessageToAllMembers(message);
        return ResponseEntity.of(
                Optional.of(Map.of(
                        "sentMessage", message
                ))
        );
    }
}
