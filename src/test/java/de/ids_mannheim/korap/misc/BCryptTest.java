package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

public class BCryptTest {

    @Test
    public void testSalt () {
        String salt = BCrypt.gensalt(8);
        // System.out.println(salt);
        String plain = "secret";
        String password = BCrypt.hashpw(plain, salt);
        // System.out.println(password);
        assertTrue(BCrypt.checkpw(plain, password));
    }
}
