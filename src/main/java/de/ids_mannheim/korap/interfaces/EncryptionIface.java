package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KustvaktException;
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
            KustvaktException;

    public String produceSecureHash(String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException,
            KustvaktException;

    public String hash(String text, String salt) throws Exception;

    public String hash(String text) throws Exception;

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

    public String validateIPAddress(String ipaddress) throws KustvaktException;

    public String validateEmail(String email) throws KustvaktException;

    public Map<String, String> validateMap(Map<String, String> map)
            throws KustvaktException;

    public String validateString(String input) throws KustvaktException;

    public void validate(Object instance) throws KustvaktException;

    public String validatePassphrase(String pw) throws KustvaktException;

}
