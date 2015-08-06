package de.ids_mannheim.korap.interfaces.defaults;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.Configurable;
import de.ids_mannheim.korap.exceptions.KorAPException;
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
@Configurable(BeanConfiguration.KUSTVAKT_ENCRYPTION)
public class DefaultEncryption implements EncryptionIface {

    private SecureRandom randomizer;

    public DefaultEncryption() {
        randomizer = new SecureRandom();
    }

    @Override
    public String produceSecureHash(String input, String salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException,
            KorAPException {
        return null;
    }

    @Override
    public String produceSecureHash(String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException,
            KorAPException {
        return null;
    }

    @Override
    public String hash(String value) {
        return null;
    }

    @Override
    public boolean checkHash(String plain, String hash, String salt) {
        return false;
    }

    @Override
    public boolean checkHash(String plain, String hash) {
        return false;
    }

    @Override
    public String getSalt(User user) {
        return null;
    }

    @Override
    public String createToken(boolean hash, Object... obj) {
        return createToken();

    }

    @Override
    public String createToken() {
        return new BigInteger(100, randomizer).toString(20);
    }

    @Override
    public String createID(Object... obj) {
        return createToken();
    }

    @Override
    public String encodeBase() {
        return null;
    }

    @Override
    public String validateIPAddress(String ipaddress) throws KorAPException {
        return null;
    }

    @Override
    public String validateEmail(String email) throws KorAPException {
        return null;
    }

    @Override
    public Map<String, Object> validateMap(Map<String, Object> map)
            throws KorAPException {
        return null;
    }

    @Override
    public String validateString(String input) throws KorAPException {
        return null;
    }

    @Override
    public void validate(Object instance) throws KorAPException {

    }

    @Override
    public String validatePassphrase(String pw) throws KorAPException {
        return null;
    }
}
