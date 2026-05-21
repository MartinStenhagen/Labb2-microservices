package org.example.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean
    RestClient userRestClient(
            @Value("${user.service.url}") String userServiceUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return RestClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
                .build();
    }
}
