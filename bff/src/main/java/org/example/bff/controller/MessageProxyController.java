package org.example.bff.controller;

import org.example.bff.dto.CreateMessageRequest;
import org.example.bff.dto.MessageServiceRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/messages")
public class MessageProxyController {

    private final RestClient messageRestClient;

    public MessageProxyController(@Qualifier("messageRestClient") RestClient messageRestClient) {
        this.messageRestClient = messageRestClient;
    }

    @PostMapping
    public Object publishMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateMessageRequest request
    ) {
        Number userId = jwt.getClaim("userId");
        MessageServiceRequest messageRequest = new MessageServiceRequest(
                userId.longValue(),
                request.content()
        );

        return messageRestClient.post()
                .uri("/messages")
                .body(messageRequest)
                .retrieve()
                .body(Object.class);
    }

    @GetMapping
    public Object getMessages() {
        return messageRestClient.get()
                .uri("/messages")
                .retrieve()
                .body(Object.class);
    }

    @GetMapping("/{id}")
    public Object getMessage(@PathVariable Long id) {
        return messageRestClient.get()
                .uri("/messages/{id}", id)
                .retrieve()
                .body(Object.class);
    }
}
