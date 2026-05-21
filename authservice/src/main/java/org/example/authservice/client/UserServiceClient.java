package org.example.authservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient userRestClient;

    public UserServiceClient(@Qualifier("userRestClient") RestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    public UserResponse createUser(String username, String displayName) {
        return userRestClient.post()
                .uri("/users")
                .body(new CreateUserRequest(username, displayName))
                .retrieve()
                .body(UserResponse.class);
    }

    private record CreateUserRequest(
            String username,
            String displayName
    ) {
    }

    public record UserResponse(
            Long id,
            String username,
            String displayName
    ) {
    }
}
