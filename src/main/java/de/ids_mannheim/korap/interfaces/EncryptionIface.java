package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.user.User;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface EncryptionIface {

    public enum Encryption {
        SIMPLE, ESAPICYPHER, BCRYPT
    }

    /**
     * One-way hashing of String input. Used to canonicalize
     *
     * @param input
     * @param salt
     * @return
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     */
    public String produceSecureHash(String input, String salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException,
            KorAPException;

    public String produceSecureHash(String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException,
            KorAPException;

    public String hash(String value);

    /**
     * @param plain
     * @param hash
     * @param salt
     * @return
     */
    public boolean checkHash(String plain, String hash, String salt);

    public boolean checkHash(String plain, String hash);

    public String getSalt(User user);

    /**
     * create random String to be used as authentication token
     *
     * @return
     */
    public String createToken(boolean hash, Object... obj);

    public String createToken();

    /**
     * create a random Integer to be used as ID for databases
     *
     * @return
     */
    public String createID(Object... obj);

    public String encodeBase();

    public String validateIPAddress(String ipaddress) throws KorAPException;

    public String validateEmail(String email) throws KorAPException;

    public Map<String, Object> validateMap(Map<String, Object> map)
            throws KorAPException;

    public String validateString(String input) throws KorAPException;

    public void validate(Object instance) throws KorAPException;

    public String validatePassphrase(String pw) throws KorAPException;

}
