package org.example.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient authRestClient(@Value("${auth.service.url}") String authServiceUrl) {
        return RestClient.builder().baseUrl(authServiceUrl).build();
    }

    @Bean
    RestClient userRestClient(@Value("${user.service.url}") String userServiceUrl) {
        return RestClient.builder().baseUrl(userServiceUrl).build();
    }

    @Bean
    RestClient messageRestClient(@Value("${message.service.url}") String messageServiceUrl) {
        return RestClient.builder().baseUrl(messageServiceUrl).build();
    }
}
