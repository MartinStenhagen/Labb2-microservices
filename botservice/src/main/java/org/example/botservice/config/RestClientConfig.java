package org.example.botservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean
    public RestClient messageServiceRestClient(
            @Value("${message.service.url}") String messageServiceUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return RestClient.builder()
                .baseUrl(messageServiceUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
                .build();
    }
}
