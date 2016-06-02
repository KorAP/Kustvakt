package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.utils.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by hanl on 29.05.16.
 */
public class StringUtilsTest {

    @Test
    public void testTextIToDoc () {
        String textSigle = "WPD_AAA.02439";
        String docSigle = "WPD_AAA";
        assertEquals(docSigle, StringUtils.getDocSigle(textSigle));
        assertEquals(docSigle, StringUtils.getDocSigle(docSigle));
    }


    @Test
    public void testSQLRegexBuild () {

    }
}
