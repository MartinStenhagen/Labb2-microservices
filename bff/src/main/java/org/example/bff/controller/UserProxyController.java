package org.example.bff.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/users")
public class UserProxyController {

    private final RestClient userRestClient;

    public UserProxyController(@Qualifier("userRestClient") RestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    @PostMapping
    public Object createUser(@RequestBody Object request) {
        return userRestClient.post()
                .uri("/users")
                .body(request)
                .retrieve()
                .body(Object.class);
    }

    @GetMapping
    public Object getUsers() {
        return userRestClient.get()
                .uri("/users")
                .retrieve()
                .body(Object.class);
    }

    @GetMapping("/{id}")
    public Object getUser(@PathVariable Long id) {
        return userRestClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .body(Object.class);
    }
}
