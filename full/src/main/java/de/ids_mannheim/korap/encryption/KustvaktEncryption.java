package de.ids_mannheim.korap.encryption;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.interfaces.EncryptionIface;

public class KustvaktEncryption implements EncryptionIface {

    private static final String ALGORITHM = "SHA-256";
    private static Logger jlog = LogManager
            .getLogger(KustvaktEncryption.class);

    private final FullConfiguration config;


    public KustvaktEncryption (FullConfiguration config) {
        jlog.info("initializing KorAPEncryption implementation");
        this.config = config;
    }


    public static boolean matchTokenByteCode (Object param) {
        if (!(param instanceof String))
            return false;
        String token = (String) param;
        byte[] bytes = token.getBytes();
        return 64 == bytes.length;
    }


    private String encodeBase (byte[] bytes) throws EncoderException {
        return Base64.encodeBase64String(bytes);
    }


    @Override
    public String encodeBase () {
        try {
            return encodeBase(this.createSecureRandom(24));
        }
        catch (EncoderException e) {
            return "";
        }
    }


    public String secureHash (String input) {
        return secureHash(input, "");
    }


    @Override
    public String secureHash (String input, String salt) {
        String hashString = "";
        switch (config.getSecureHashAlgorithm()) {
            case ESAPICYPHER:
                break;
            case SIMPLE:
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(input.getBytes("UTF-8"));
                    byte[] digest = md.digest();

                    for (byte b : digest)
                        hashString += String.format("%02x", b);
                }
                catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                break;
            case BCRYPT:
                hashString = bcryptHash(input, salt);
                break;
            default:
                jlog.warn("Invalid value: "+ config.getSecureHashAlgorithm());
                break;
        }
        return hashString;
    }



    public String hash (String input) {
        String hashString = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ALGORITHM);
            md.update(input.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e) {
            return "";
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }

        byte[] digest = md.digest();

        for (byte b : digest) {
            hashString += String.format("%02x", b);
        }
        return hashString;
    }


    /**
     * // some sort of algorithm to create token and isSystem
     * regularly the integrity
     * // of the token
     * public String createAuthToken() {
     * final byte[] rNumber = SecureRGenerator
     * .getNextSecureRandom(SecureRGenerator.TOKEN_RANDOM_SIZE);
     * String hash;
     * try {
     * hash = produceSimpleHash(SecureRGenerator.toHex(rNumber));
     * } catch (NoSuchAlgorithmException |
     * UnsupportedEncodingException e) {
     * return "";
     * }
     * return hash;
     * }
     */

    private byte[] createSecureRandom (int size) {
        return SecureRGenerator.getNextSecureRandom(size);
    }


    /**
     * does this need to be equal for every iteration?!
     * @param hash
     * @param obj
     * @return
     */
    @Override
    public String createToken (boolean hash, Object ... obj) {
        StringBuffer b = new StringBuffer();
        try {
            for (Object o : obj) {
                b.append(" | ");
                b.append(o);
            }
            if (hash)
                return encodeBase(hash(b.toString().trim()).getBytes());
            else
                return encodeBase(b.toString().trim().getBytes());
        }
        catch (EncoderException e) {
            return "";
        }

    }


    @Override
    public String createToken () {
        return RandomStringUtils.randomAlphanumeric(64);
        
        // EM: code from MH
//        String encoded;
//        String v = RandomStringUtils.randomAlphanumeric(64);
//        encoded = hash(v);
//        jlog.trace("creating new token {}", encoded);
//        return encoded;
    }


    @Override
    public String createRandomNumber (Object ... obj) {
        final byte[] rNumber = SecureRGenerator
                .getNextSecureRandom(SecureRGenerator.ID_RANDOM_SIZE);
        if (obj.length == 0) {
            obj = new Object[1];
            obj[0] = rNumber;
        }
        return createToken(false, obj);
    }


    @Override
    public boolean checkHash (String plain, String hash, String salt) {
        String pw = "";
        switch (config.getSecureHashAlgorithm()) {
            case ESAPICYPHER:
                pw = secureHash(plain, salt);
                break;
            case BCRYPT:
                try {
                    return BCrypt.checkpw(plain, hash);
                }
                catch (IllegalArgumentException e) {
                    return false;
                }
            case SIMPLE:
                pw = hash(plain);
                break;
        }
        return pw.equals(hash);
    }


    @Override
    public boolean checkHash (String plain, String hash) {
        switch (config.getSecureHashAlgorithm()) {
            case ESAPICYPHER:
                return secureHash(plain).equals(hash);
            case BCRYPT:
                try {
                    return BCrypt.checkpw(plain, hash);
                }
                catch (IllegalArgumentException e) {
                    return false;
                }
            case SIMPLE:
                return hash(plain).equals(hash);
        }
        return false;
    }


    private String bcryptHash (String text, String salt) {
        if (salt == null || salt.isEmpty())
            salt = BCrypt.gensalt(config.getLoadFactor());
        return BCrypt.hashpw(text, salt);
    }


    @Override
    public String toString () {
        return this.getClass().getCanonicalName();
    }

    public static class SecureRGenerator {
        private static final String SHA1_PRNG = "SHA1PRNG";
        protected static final int DEFAULT_RANDOM_SIZE = 128;
        protected static final int TOKEN_RANDOM_SIZE = 128;
        protected static final int ID_RANDOM_SIZE = 128;
        protected static final int CORPUS_RANDOM_SIZE = 64;
        private static final char[] HEX_DIGIT = { '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'z', 'x',
                'h', 'q', 'w' };
        private static final SecureRandom sRandom__;

        static {
            try {
                sRandom__ = SecureRandom.getInstance("SHA1PRNG");
            }
            catch (NoSuchAlgorithmException e) {
                throw new Error(e);
            }
        }


        public static byte[] getNextSecureRandom (int bits) {
            if (bits % 8 != 0) {
                throw new IllegalArgumentException(
                        "Size is not divisible by 8!");
            }

            byte[] bytes = new byte[bits / 8];

            sRandom__.nextBytes(bytes);

            return bytes;
        }


        public static String toHex (byte[] bytes) {
            if (bytes == null) {
                return null;
            }

            StringBuilder buffer = new StringBuilder(bytes.length * 2);
            for (byte thisByte : bytes) {
                buffer.append(byteToHex(thisByte));
            }

            return buffer.toString();
        }


        private static String byteToHex (byte b) {
            char[] array = { HEX_DIGIT[(b >> 4 & 0xF)], HEX_DIGIT[(b & 0xF)] };
            return new String(array);
        }
    }

}
