package de.ids_mannheim.korap.authentication;

import static org.junit.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;

public class RandomCodeGeneratorTest extends SpringJerseyTest {

    @Autowired
    private RandomCodeGenerator random;

    @Test
    public void testRandomGenerator ()
            throws NoSuchAlgorithmException, KustvaktException {
        String value = random.createRandomCode();
        assertEquals(22, value.length());
        //System.out.println(value);
    }

    @Ignore
    public void testRandomGeneratorPerformance () throws OAuthSystemException,
            NoSuchAlgorithmException, KustvaktException {
        long min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

        String code;
        while (true) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                code = random.createRandomCode();
                code = random.filterRandomCode(code);
            }
            long end = System.currentTimeMillis();
            long duration = end - start;
            if (duration < min)
                min = duration;
            else if (duration > max) max = duration;
            System.out.println(
                    "d : " + duration + " min :" + min + ", max: " + max);

        }
    }

    @Ignore
    public void testMD5Generator () throws OAuthSystemException,
            NoSuchAlgorithmException, KustvaktException {
        MD5Generator m = new MD5Generator();
        String value = m.generateValue();
        value = m.generateValue(value);
        System.out.println(value);
    }

}
