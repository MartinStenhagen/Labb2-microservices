package org.example.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean
    RestClient authRestClient(
            @Value("${auth.service.url}") String authServiceUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return internalRestClient(authServiceUrl, internalApiKey);
    }

    @Bean
    RestClient userRestClient(
            @Value("${user.service.url}") String userServiceUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return internalRestClient(userServiceUrl, internalApiKey);
    }

    @Bean
    RestClient messageRestClient(
            @Value("${message.service.url}") String messageServiceUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return internalRestClient(messageServiceUrl, internalApiKey);
    }

    private RestClient internalRestClient(String baseUrl, String internalApiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
                .build();
    }
}
