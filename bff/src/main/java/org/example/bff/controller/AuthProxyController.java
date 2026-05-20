package org.example.bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class AuthProxyController {

    private final RestClient authRestClient;

    public AuthProxyController(@Qualifier("authRestClient") RestClient authRestClient) {
        this.authRestClient = authRestClient;
    }

    @PostMapping("/login")
    public Object login(@RequestBody Object request) {
        return authRestClient.post()
                .uri("/auth/login")
                .body(request)
                .retrieve()
                .body(Object.class);
    }
}
