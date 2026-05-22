package org.example.botservice.service;

import org.example.event.MessagePublishedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedBotReplyGeneratorTest {

    private final RuleBasedBotReplyGenerator generator = new RuleBasedBotReplyGenerator();

    @Test
    void generateReplyGreetsUser() {
        String reply = generator.generateReply(event("Hej @bot"));

        assertThat(reply).isEqualTo("Hej martin! Kul att se dig. Vad vill du prata om?");
    }

    @Test
    void generateReplyHasHelpSpecificAnswer() {
        String reply = generator.generateReply(event("@bot hjälp mig"));

        assertThat(reply).contains("Jag kan hjälpa till med frågor");
    }

    @Test
    void generateReplyAnswersHowBotIsDoing() {
        String reply = generator.generateReply(event("@bot hur mår du?"));

        assertThat(reply).isEqualTo("Jag mår bra, tack! Hur är läget med dig?");
    }

    @Test
    void generateReplyAnswersThanks() {
        String reply = generator.generateReply(event("@bot tack"));

        assertThat(reply).isEqualTo("Varsågod, martin!");
    }

    @Test
    void generateReplyHasGenericChattyFallback() {
        String reply = generator.generateReply(event("@bot jag funderar på middag"));

        assertThat(reply).isEqualTo("Jag är med, martin. Berätta lite mer så försöker jag svara.");
    }

    private MessagePublishedEvent event(String content) {
        return new MessagePublishedEvent(
                UUID.randomUUID(),
                1L,
                1L,
                "martin",
                "general",
                content,
                LocalDateTime.now()
        );
    }
}
