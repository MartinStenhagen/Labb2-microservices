package org.example.botservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE_NAME = "chat.exchange";
    public static final String ROUTING_KEY = "message.published";
    public static final String QUEUE_NAME = "bot.messages.queue";
    public static final String DEAD_LETTER_EXCHANGE_NAME = "chat.dead-letter.exchange";
    public static final String DEAD_LETTER_ROUTING_KEY = "message.published.dead-letter";
    public static final String DEAD_LETTER_QUEUE_NAME = "bot.messages.dead-letter.queue";

    @Bean
    public Queue botMessagesQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE_NAME)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange botDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE_NAME);
    }

    @Bean
    public Queue botMessagesDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE_NAME).build();
    }

    @Bean
    public Binding botMessagesBinding(Queue botMessagesQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(botMessagesQueue).to(chatExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding botMessagesDeadLetterBinding(Queue botMessagesDeadLetterQueue, DirectExchange botDeadLetterExchange) {
        return BindingBuilder.bind(botMessagesDeadLetterQueue)
                .to(botDeadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
