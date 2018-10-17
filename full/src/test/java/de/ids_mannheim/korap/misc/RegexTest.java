package de.ids_mannheim.korap.misc;

import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;

import org.junit.Test;

import de.ids_mannheim.korap.annotation.AnnotationParser;

public class RegexTest {

    @Test
    public void testQuote(){
        String s = "ah[\"-\"]";
        Matcher m = AnnotationParser.quotePattern.matcher(s);
        if (m.find()){
            assertEquals("-",m.group(1));
        }
    }
    
}
