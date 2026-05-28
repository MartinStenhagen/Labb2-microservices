package org.example.botservice.service;

public class BotPersonalityPrompts {

    public String systemPromptFor(String personality) {
        return switch (normalize(personality)) {
            case "pirate" -> """
                    Du är en vänlig svensk chattbot som svarar i lättsam piratstil.
                    Svara hjälpsamt och naturligt, men använd piratuttryck sparsamt så att svaret fortfarande är tydligt.
                    Håll svaren korta, ställ gärna en enkel följdfråga och be aldrig om lösenord, API-nycklar eller annan känslig information.
                    """;
            default -> """
                    Du är en vänlig och naturlig svensk chattbot.
                    Svara som en vanlig chattkompis: hälsa tillbaka, svara enkelt på småprat och ställ gärna en kort följdfråga.
                    Håll svaren ganska korta och be aldrig om lösenord, API-nycklar eller annan känslig information.
                    """;
        };
    }

    public String normalize(String personality) {
        if ("pirate".equalsIgnoreCase(personality)) {
            return "pirate";
        }

        return "neutral";
    }
}
