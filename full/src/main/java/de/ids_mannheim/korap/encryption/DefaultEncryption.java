package de.ids_mannheim.korap.encryption;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.Configurable;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.user.User;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

/**
 * @author hanl
 * @date 05/06/2015
 */
@Configurable(ContextHolder.KUSTVAKT_ENCRYPTION)
public class DefaultEncryption implements EncryptionIface {

    private SecureRandom randomizer;


    public DefaultEncryption () {
        randomizer = new SecureRandom();
    }


    @Override
    public String secureHash (String input, String salt)
            throws KustvaktException {
        return null;
    }


    @Override
    public String secureHash (String input) throws NoSuchAlgorithmException,
            UnsupportedEncodingException, KustvaktException {
        return null;
    }


    @Override
    public boolean checkHash (String plain, String hash, String salt) {
        return false;
    }


    @Override
    public boolean checkHash (String plain, String hash) {
        return false;
    }


    @Override
    public String createToken (boolean hash, Object ... obj) {
        return createToken();

    }


    @Override
    public String createToken () {
        return new BigInteger(100, randomizer).toString(20);
    }


    @Override
    public String createRandomNumber (Object ... obj) {
        return createToken();
    }


    @Override
    public String encodeBase () {
        return null;
    }

}
