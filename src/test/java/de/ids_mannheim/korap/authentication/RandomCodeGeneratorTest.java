package de.ids_mannheim.korap.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
        // System.out.println(value);
    }

    @Disabled
    public void testRandomGeneratorPerformance ()
            throws NoSuchAlgorithmException, KustvaktException {
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
            else if (duration > max)
                max = duration;
            System.out.println(
                    "d : " + duration + " min :" + min + ", max: " + max);
        }
    }
}
