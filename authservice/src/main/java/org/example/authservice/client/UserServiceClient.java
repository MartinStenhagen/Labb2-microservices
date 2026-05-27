package org.example.authservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class UserServiceClient {

    private final RestClient userRestClient;

    public UserServiceClient(@Qualifier("userRestClient") RestClient userRestClient) {
        this.userRestClient = userRestClient;
    }

    public UserResponse createUser(String username, String displayName) {
        return userRestClient.post()
                .uri("/users")
                .body(Map.of(
                        "username", username,
                        "displayName", displayName
                ))
                .retrieve()
                .body(UserResponse.class);
    }

    public static class UserResponse {
        private Long id;
        private String username;
        private String displayName;

        public UserResponse() {
        }

        public UserResponse(Long id, String username, String displayName) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
