package org.example.messageservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.messageservice.model.ChatMessage;
import org.example.messageservice.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ChatMessage publishMessage(@RequestBody ChatMessage message) throws JsonProcessingException {
        return messageService.publishMessage(message);
    }

    @GetMapping("/{id}")
    public ChatMessage getMessage(@PathVariable Long id) {
        return messageService.getMessage(id);
    }

    @GetMapping
    public List<ChatMessage> getMessages(@RequestParam(defaultValue = "general") String room) {
        return messageService.getMessages(room);
    }
}
