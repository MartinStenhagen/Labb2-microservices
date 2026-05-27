package org.example.messageservice;

import org.example.event.MessagePublishedEvent;
import org.example.messageservice.model.ChatMessage;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ImportGrpcClients(basePackages = "org.example.grpc.user")
@RegisterReflectionForBinding({
        ChatMessage.class,
        MessagePublishedEvent.class
})
public class MessageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
