package org.example.botservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient messageServiceRestClient(@Value("${message.service.url}") String messageServiceUrl) {
        return RestClient.builder()
                .baseUrl(messageServiceUrl)
                .build();
    }
}
