package org.example.botservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    @Test
    void botMessagesQueueUsesDeadLetterQueue() {
        RabbitConfig rabbitConfig = new RabbitConfig();

        var queue = rabbitConfig.botMessagesQueue();

        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitConfig.DEAD_LETTER_EXCHANGE_NAME)
                .containsEntry("x-dead-letter-routing-key", RabbitConfig.DEAD_LETTER_ROUTING_KEY);
    }
}
