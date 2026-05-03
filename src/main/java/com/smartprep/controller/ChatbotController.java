package com.smartprep.controller;

import com.smartprep.dto.ChatMessage;
import com.smartprep.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatMessage chatMessage) {
        log.info("Received chat message: userId={}, message={}", chatMessage.getUserId(), chatMessage.getMessage());
        String reply = chatbotService.chat(chatMessage.getUserId(), chatMessage.getMessage());
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}