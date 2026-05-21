package org.example.botservice.service;

import org.example.event.MessagePublishedEvent;
import org.springframework.util.StringUtils;

import java.util.Locale;

public class RuleBasedBotReplyGenerator {

    public String generateReply(MessagePublishedEvent event) {
        String message = normalizeMessage(event.content());
        String name = StringUtils.hasText(event.senderUsername()) ? event.senderUsername() : "där";

        if (!StringUtils.hasText(message)) {
            return "Hej " + name + "! Jag är här. Skriv gärna vad du vill ha hjälp med.";
        }

        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
        if (lowerCaseMessage.contains("hjälp") || lowerCaseMessage.contains("help")) {
            return "Absolut, " + name + ". Jag kan hjälpa till med frågor, idéer, förklaringar eller bara bolla något.";
        }

        if (lowerCaseMessage.contains("test")) {
            return "Jag är igång, " + name + ". Meddelandet kom fram.";
        }

        if (saysThanks(lowerCaseMessage)) {
            return "Varsågod, " + name + "!";
        }

        if (saysGoodbye(lowerCaseMessage)) {
            return "Vi hörs, " + name + "!";
        }

        if (asksHowBotIsDoing(lowerCaseMessage)) {
            return "Jag mår bra, tack! Hur är läget med dig?";
        }

        if (isGreeting(lowerCaseMessage)) {
            return "Hej " + name + "! Kul att se dig. Vad vill du prata om?";
        }

        return "Jag är med, " + name + ". Berätta lite mer så försöker jag svara.";
    }

    private boolean isGreeting(String message) {
        return message.matches("^(hej|hejsan|hallå|hallo|tja|tjena|god morgon|god kväll|goddag)(\\b|[!,. ]).*");
    }

    private boolean asksHowBotIsDoing(String message) {
        return message.contains("hur mår du")
                || message.contains("hur är läget")
                || message.contains("läget")
                || message.contains("vad händer");
    }

    private boolean saysThanks(String message) {
        return message.contains("tack")
                || message.contains("schysst")
                || message.contains("snällt");
    }

    private boolean saysGoodbye(String message) {
        return message.contains("hejdå")
                || message.contains("hej då")
                || message.contains("vi hörs")
                || message.contains("godnatt");
    }

    private String normalizeMessage(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replaceAll("(?i)@bot", "")
                .replaceAll("\\s+([,.!?])", "$1")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}
