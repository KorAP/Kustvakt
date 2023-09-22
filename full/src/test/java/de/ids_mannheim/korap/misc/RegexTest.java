package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import de.ids_mannheim.korap.annotation.AnnotationParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Regex Test")
class RegexTest {

    @Test
    @DisplayName("Test Quote")
    void testQuote() {
        String s = "ah[\"-\"]";
        Matcher m = AnnotationParser.quotePattern.matcher(s);
        if (m.find()) {
            assertEquals(m.group(1), "-");
        }
    }
}
