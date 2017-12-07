package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.authentication.BasicAuthentication;
import de.ids_mannheim.korap.authentication.http.AuthorizationData;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.authentication.http.TransferEncoding;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.StringUtils;

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
    public void testBasicHttpSplit () throws KustvaktException {
        TransferEncoding transferEncoding = new TransferEncoding();
        String s2 = new String(Base64.encodeBase64("test:testPass".getBytes()));
        String[] f2 = transferEncoding.decodeBase64(s2);
        assertEquals("test", f2[0]);
        assertEquals("testPass", f2[1]);


        HttpAuthorizationHandler handler = new HttpAuthorizationHandler();
        String s1 = "basic "
                + new String(Base64.encodeBase64("test:testPass".getBytes()));
        AuthorizationData f1 = handler.parseAuthorizationHeaderValue(s1);
        assertEquals(s2, f1.getToken());
    }

}
