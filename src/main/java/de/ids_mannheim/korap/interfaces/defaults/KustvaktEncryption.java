package de.ids_mannheim.korap.interfaces.defaults;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.user.User;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.mindrot.jbcrypt.BCrypt;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Randomizer;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.owasp.esapi.reference.DefaultValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KustvaktEncryption implements EncryptionIface {

    private static final String ALGORITHM = "SHA-256";
    private static Logger jlog = LoggerFactory
            .getLogger(KustvaktEncryption.class);
    // todo: disable this
    private static final String PASSWORD_SALT_FIELD = "accountCreation";

    private final boolean nullable;
    private final Validator validator;
    private final Randomizer randomizer;
    private KustvaktConfiguration config;

    public KustvaktEncryption(KustvaktConfiguration config) {
        jlog.info("initializing KorAPEncryption implementation");
        this.nullable = false;
        this.validator = DefaultValidator.getInstance();
        this.randomizer = ESAPI.randomizer();
        this.config = config;
    }

    public static boolean matchTokenByteCode(Object param) {
        if (!(param instanceof String))
            return false;
        String token = (String) param;
        byte[] bytes = token.getBytes();
        return 64 == bytes.length;
    }

    private String encodeBase(byte[] bytes) throws EncoderException {
        return Base64.encodeBase64String(bytes);
    }

    @Override
    public String encodeBase() {
        try {
            return encodeBase(this.createSecureRandom(24));
        }catch (EncoderException e) {
            return "";
        }
    }

    public String produceSecureHash(String input) {
        return produceSecureHash(input, "");
    }

    @Override
    public String produceSecureHash(String input, String salt) {
        String hashString = "";
        switch (config.getEncryption()) {
            case ESAPICYPHER:
                try {
                    hashString = hash(input, salt);
                }catch (NoSuchAlgorithmException e) {
                    jlog.error("there was an encryption error!", e);
                    return null;
                }catch (Exception e) {
                    jlog.error("there was an error!", e);
                    return null;
                }
                break;
            case SIMPLE:
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(input.getBytes("UTF-8"));
                    byte[] digest = md.digest();

                    for (byte b : digest)
                        hashString += String.format("%02x", b);
                }catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                break;
            case BCRYPT:
                hashString = bcryptHash(input, salt);
                break;
            default:
                jlog.warn("Invalid value: {}", config.getEncryption());
                break;
        }
        return hashString;
    }

    public String hash(String text, String salt) throws Exception {
        byte[] bytes;

        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        md.reset();
        md.update(ESAPI.securityConfiguration().getMasterSalt());
        md.update(salt.getBytes());
        md.update(text.getBytes());

        bytes = md.digest();
        for (int i = 0; i < 234; ++i) {
            md.reset();
            bytes = md.digest(bytes);
        }
        String coding = ESAPI.encoder().encodeForBase64(bytes, false);
        return coding;
    }

    @Override
    public String hash(String input) {
        String hashString = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(input.getBytes("UTF-8"));
        }catch (NoSuchAlgorithmException e) {
            return "";
        }catch (UnsupportedEncodingException e) {
            return "";
        }

        byte[] digest = md.digest();

        for (byte b : digest) {
            hashString += String.format("%02x", b);
        }
        return hashString;
    }

    /**
     * // some sort of algorithm to create token and isSystem regularly the integrity
     * // of the token
     * public String createAuthToken() {
     * final byte[] rNumber = SecureRGenerator
     * .getNextSecureRandom(SecureRGenerator.TOKEN_RANDOM_SIZE);
     * String hash;
     * try {
     * hash = produceSimpleHash(SecureRGenerator.toHex(rNumber));
     * } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
     * return "";
     * }
     * return hash;
     * }
     */

    private byte[] createSecureRandom(int size) {
        return SecureRGenerator.getNextSecureRandom(size);
    }

    @Override
    public String createToken(boolean hash, Object... obj) {
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
        }catch (EncoderException e) {
            return "";
        }

    }

    @Override
    public String createToken() {
        String encoded;
        String v = randomizer
                .getRandomString(SecureRGenerator.TOKEN_RANDOM_SIZE,
                        SecureRGenerator.toHex(createSecureRandom(64))
                                .toCharArray());
        encoded = hash(v);
        jlog.trace("creating new token {}", encoded);
        return encoded;
    }

    @Override
    public String createID(Object... obj) {
        final byte[] rNumber = SecureRGenerator
                .getNextSecureRandom(SecureRGenerator.CORPUS_RANDOM_SIZE);
        if (obj.length != 0) {
            ArrayList s = new ArrayList();
            Collections.addAll(s, obj);
            obj = s.toArray();
        }else {
            obj = new Object[1];
            obj[0] = rNumber;
        }
        return createToken(false, obj);
    }

    @Override
    public boolean checkHash(String plain, String hash, String salt) {
        String pw = "";
        switch (config.getEncryption()) {
            case ESAPICYPHER:
                pw = produceSecureHash(plain, salt);
                break;
            case BCRYPT:
                try {
                    return BCrypt.checkpw(plain, hash);
                }catch (IllegalArgumentException e) {
                    return false;
                }
            case SIMPLE:
                pw = hash(plain);
                break;
        }
        return pw.equals(hash);
    }

    @Override
    public boolean checkHash(String plain, String hash) {
        switch (config.getEncryption()) {
            case ESAPICYPHER:
                return produceSecureHash(plain).equals(hash);
            case BCRYPT:
                try {
                    return BCrypt.checkpw(plain, hash);
                }catch (IllegalArgumentException e) {
                    return false;
                }
            case SIMPLE:
                return hash(plain).equals(hash);
        }
        return false;
    }

    @Override
    public String getSalt(User user) {
        Class u = user.getClass();
        Field field;
        try {
            field = u.getSuperclass().getDeclaredField(PASSWORD_SALT_FIELD);
        }catch (NoSuchFieldException e) {
            try {
                field = u.getDeclaredField(PASSWORD_SALT_FIELD);
            }catch (NoSuchFieldException e1) {
                // do nothing
                e.printStackTrace();
                return null;
            }
        }
        try {
            field.setAccessible(true);
            String value = String.valueOf(field.get(user));
            field.setAccessible(false);
            return value;
        }catch (IllegalAccessException e) {
            // do nothing
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, Object> validateMap(Map<String, Object> map)
            throws KustvaktException {
        Map<String, Object> safeMap = new HashMap<>();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = null;
                if (entry.getValue() instanceof String) {
                    value = validateString((String) entry.getValue());

                }else if (entry.getValue() instanceof List) {
                    List list = (List) entry.getValue();
                    for (Object v : list) {
                        if (v instanceof String)
                            validateString((String) v);
                    }

                    if (((List) entry.getValue()).size() == 1)
                        value = list.get(0);
                    else
                        value = list;
                }
                safeMap.put(entry.getKey(), value);
            }
        }
        return safeMap;
    }


    private String validateString(String descr, String input, String type,
            int length, boolean nullable) throws KustvaktException {
        if (jlog.isDebugEnabled())
            jlog.debug("validating string entry '{}'", input);
        String s;
        try {
            s = validator.getValidInput(descr, input, type, length, nullable);
        }catch (ValidationException e) {
            jlog.error(
                    "String value did not validate ('{}') with validation type {}",
                    new Object[] { input, type, e.getMessage() });
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "invalid string of type " + type, input);
        }
        return s;
    }

    @Override
    public String validateString(String input) throws KustvaktException {
        if (input.contains("@")) {
            return validateEmail(input);
        }else
            return validateString("Safe String", input, "SafeString",
                    config.getValidationStringLength(), nullable);
    }

    @Override
    public String validateEmail(String email) throws KustvaktException {
        jlog.debug("validating email entry '{}'", email);
        return validateString("Email", email, "Email",
                config.getValidationEmaillength(), nullable);
    }

    @Override
    public String validateIPAddress(String ipaddress) throws KustvaktException {
        jlog.debug("validating IP address entry '{}'", ipaddress);
        return validateString("IP Address", ipaddress, "IPAddress",
                config.getValidationStringLength(), nullable);
    }

    @Override
    public void validate(Object instance) throws KustvaktException {
        if (instance == null)
            return;
        try {
            validateStringField(instance.getClass().getDeclaredFields(),
                    instance);
            validateStringField(
                    instance.getClass().getSuperclass().getDeclaredFields(),
                    instance);
        }catch (IllegalAccessException e) {
            jlog.error("object value did not validate", e.getMessage());
            throw new KustvaktException(StatusCodes.PARAMETER_VALIDATION_ERROR,
                    "object could not be validated", instance.toString());
        }
    }

    //fixme: fix validation algorithm
    @Override
    public String validatePassphrase(String pw) throws KustvaktException {
        String safe_string = validateString(pw);
        String pw_conf;
        try {
            pw_conf = validator
                    .getValidInput("User Password", safe_string, "Password", 20,
                            false);
        }catch (ValidationException e) {
            jlog.error("password value did not validate", e.getMessage());
            throw new KustvaktException(StatusCodes.PARAMETER_VALIDATION_ERROR,
                    "password did not validate", "password");
        }
        return pw_conf;
    }

    //FIXME: currently all sets are skipped during validation (since users should not be allowed to edit those sets anyway,
    //I think we will be safe here
    private void validateStringField(Field[] fields, Object instance)
            throws KustvaktException, IllegalAccessException {
        for (Field field : fields) {
            boolean priv = false;
            if (field.getType().isAssignableFrom(String.class)) {
                if (Modifier.isPrivate(field.getModifiers())) {
                    priv = true;
                    field.setAccessible(true);
                }
                if (field.getName().equals("password") | Modifier
                        .isFinal(field.getModifiers()))
                    continue;
                String val = (String) field.get(instance);
                if (val != null) {
                    String[] set = val.split(";");
                    if (set.length > 1)
                        continue;
                }
                String safe;
                if (!field.getName().equals("email"))
                    safe = validateString("Safe String", val, "SafeString",
                            config.getValidationStringLength(), true);
                else
                    safe = validateString("User Email", val, "Email",
                            config.getValidationEmaillength(), true);
                field.set(instance, safe == null ? "" : safe);

                if (priv) {
                    field.setAccessible(false);
                }
            }
        }
    }

    private String bcryptHash(String text, String salt) {
        if (salt == null || salt.isEmpty())
            salt = BCrypt.gensalt(config.getLoadFactor());
        return BCrypt.hashpw(text, salt);
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName();
    }

    public static class SecureRGenerator {
        private static final String SHA1_PRNG = "SHA1PRNG";
        protected static final int DEFAULT_RANDOM_SIZE = 128;
        protected static final int TOKEN_RANDOM_SIZE = 128;
        protected static final int USERID_RANDOM_SIZE = 64;
        protected static final int CORPUS_RANDOM_SIZE = 48;
        private static final char[] HEX_DIGIT = { '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'z', 'x', 'h',
                'q', 'w' };
        private static final SecureRandom sRandom__;

        static {
            try {
                sRandom__ = SecureRandom.getInstance("SHA1PRNG");
            }catch (NoSuchAlgorithmException e) {
                throw new Error(e);
            }
        }

        public static byte[] getNextSecureRandom(int bits) {
            if (bits % 8 != 0) {
                throw new IllegalArgumentException(
                        "Size is not divisible by 8!");
            }

            byte[] bytes = new byte[bits / 8];

            sRandom__.nextBytes(bytes);

            return bytes;
        }

        public static String toHex(byte[] bytes) {
            if (bytes == null) {
                return null;
            }

            StringBuilder buffer = new StringBuilder(bytes.length * 2);
            for (byte thisByte : bytes) {
                buffer.append(byteToHex(thisByte));
            }

            return buffer.toString();
        }

        private static String byteToHex(byte b) {
            char[] array = { HEX_DIGIT[(b >> 4 & 0xF)], HEX_DIGIT[(b & 0xF)] };
            return new String(array);
        }
    }

}
