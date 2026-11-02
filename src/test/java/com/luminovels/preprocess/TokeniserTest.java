package com.luminovels.preprocess;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokeniserTest {

    private final Tokeniser tokeniser = new Tokeniser(StopWords.defaults());

    @Test
    void lowerCasesSplitsAndDropsStopWordsNumbersAndSingleLetters() {
        List<String> tokens = tokeniser.tokenise("The Hero's sword, 2 swords and MAGIC!");
        assertEquals(List.of("hero", "sword", "swords", "magic"), tokens);
    }

    @Test
    void dropsReportSpecificStopWords() {
        assertEquals(List.of("dragon"), tokeniser.tokenise("can source dragon"));
    }

    @Test
    void emptyAndNullGiveNoTokens() {
        assertTrue(tokeniser.tokenise("").isEmpty());
        assertTrue(tokeniser.tokenise(null).isEmpty());
    }

    @Test
    void normaliserStripsTagsAndCredits() {
        String cleaned = new Normaliser().normalise("A <br>knight  story.\n(Source: Yen Press) [Written by MAL Rewrite]");
        assertEquals("A knight story.", cleaned);
    }
}
