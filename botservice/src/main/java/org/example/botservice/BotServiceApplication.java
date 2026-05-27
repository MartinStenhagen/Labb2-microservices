package org.example.botservice;

import org.example.event.MessagePublishedEvent;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@RegisterReflectionForBinding(MessagePublishedEvent.class)
public class BotServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotServiceApplication.class, args);
    }
}
