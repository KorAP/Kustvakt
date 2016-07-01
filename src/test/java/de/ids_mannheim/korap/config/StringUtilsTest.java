package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    public void testBasicHttpSplit() {
            String s1 = "basic " + new String(Base64.encodeBase64("test:testPass".getBytes()));
            String s2 = new String(Base64.encodeBase64("test:testPass".getBytes()));
            String[] f1 = BasicHttpAuth.decode(s1);
            String[] f2 = BasicHttpAuth.decode(s2);
            assertNotNull(f1);
            assertNotNull(f2);
            assertEquals("test", f1[0]);
            assertEquals("testPass", f1[1]);
            assertEquals("test", f2[0]);
            assertEquals("testPass", f2[1]);
    }

}
