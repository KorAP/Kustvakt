package de.ids_mannheim.korap.interfaces.defaults;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.web.utils.KustvaktMap;
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


    private final boolean nullable;
    private final Validator validator;
    private final Randomizer randomizer;
    private KustvaktConfiguration config;


    public KustvaktEncryption (KustvaktConfiguration config) {
        jlog.info("initializing KorAPEncryption implementation");
        this.nullable = false;
        this.validator = DefaultValidator.getInstance();
        this.randomizer = ESAPI.randomizer();
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
        switch (config.getEncryption()) {
            case ESAPICYPHER:
                try {
                    hashString = hash(input, salt);
                }
                catch (NoSuchAlgorithmException e) {
                    jlog.error("there was an encryption error!", e);
                    return null;
                }
                catch (Exception e) {
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
                }
                catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
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



    public String hash (String text, String salt) throws Exception {
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


    public String hash (String input) {
        String hashString = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
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
        String encoded;
        String v = randomizer.getRandomString(
                SecureRGenerator.TOKEN_RANDOM_SIZE,
                SecureRGenerator.toHex(createSecureRandom(64)).toCharArray());
        encoded = hash(v);
        jlog.trace("creating new token {}", encoded);
        return encoded;
    }


    @Override
    public String createRandomNumber (Object ... obj) {
        final byte[] rNumber = SecureRGenerator
                .getNextSecureRandom(SecureRGenerator.CORPUS_RANDOM_SIZE);
        if (obj.length != 0) {
            ArrayList s = new ArrayList();
            Collections.addAll(s, obj);
            obj = s.toArray();
        }
        else {
            obj = new Object[1];
            obj[0] = rNumber;
        }
        return createToken(false, obj);
    }


    @Override
    public boolean checkHash (String plain, String hash, String salt) {
        String pw = "";
        switch (config.getEncryption()) {
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
        switch (config.getEncryption()) {
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


    // todo: where applied?
    @Override
    public Map<String, Object> validateMap (Map<String, Object> map)
            throws KustvaktException {
        Map<String, Object> safeMap = new HashMap<>();
        KustvaktMap kmap = new KustvaktMap(map);

        if (map != null) {
            if (!kmap.isGeneric()) {
                for (String key : kmap.keySet()) {
                    String value = validateEntry(kmap.get(key), key);
                    safeMap.put(key, value);
                }
            }
            else {
                for (String key : kmap.keySet()) {
                    Object value = kmap.getRaw(key);
                    if (value instanceof String) {
                        value = validateEntry((String) value, key);

                    }
                    else if (value instanceof List) {
                        List list = (List) value;
                        for (Object v : list) {
                            if (v instanceof String)
                                validateEntry((String) v, key);
                        }

                        if (list.size() == 1)
                            value = list.get(0);
                        else
                            value = list;
                    }
                    safeMap.put(key, value);
                }
            }
        }
        return safeMap;
    }


    @Deprecated
    private String validateString (String descr, String input, String type,
            int length, boolean nullable) throws KustvaktException {
        String s;
        try {
            s = validator.getValidInput(descr, input, type, length, nullable);
        }
        catch (ValidationException e) {
            jlog.error(
                    "String value did not validate ('{}') with validation type {}",
                    new Object[] { input, type, e.getMessage() });
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "invalid string of type " + type, input);
        }
        return s;
    }


    @Override
    public String validateEntry (String input, String type)
            throws KustvaktException {
        try {
            if (type != null) {
                type = type.toLowerCase();
                if (type.equals(Attributes.EMAIL)) {
                    jlog.debug("validating email entry '{}'", input.hashCode());
                    return validator.getValidInput("Email", input, "email",
                            config.getValidationEmaillength(), false);
                }
                else if (type.equals(Attributes.USERNAME)) {
                    jlog.debug("validating username entry '{}'",
                            input.hashCode());
                    return validator.getValidInput("Username", input,
                            "username", config.getValidationEmaillength(),
                            false);
                }
                else if (type.equals(Attributes.IP_RANG)) {
                    jlog.debug("validating ip address entry '{}'",
                            input.hashCode());
                    return validator.getValidInput("IP Address", input,
                            "ipaddress", config.getValidationStringLength(),
                            nullable);
                }
                else if (type.equals(Attributes.PASSWORD)) {
                    jlog.debug("validating password entry '{}'",
                            input.hashCode());
                    return validator.getValidInput("Password", input,
                            "password", config.getValidationStringLength(),
                            nullable);
                }
            }
            jlog.debug("validating string entry '{}'", input.hashCode());
            return validator.getValidInput("Safe String", input, "SafeString",
                    config.getValidationStringLength(), nullable);
        }
        catch (ValidationException ex) {
            jlog.error("Validation failed! Value '{}' with type '{}'",
                    new Object[] { input, type });
            throw new KustvaktException(StatusCodes.PARAMETER_VALIDATION_ERROR,
                    "invalid value of type " + type, input);
        }
    }


    public void validate (Object instance) throws KustvaktException {
        if (instance == null)
            return;
        try {
            validateStringField(instance.getClass().getDeclaredFields(),
                    instance);
            validateStringField(instance.getClass().getSuperclass()
                    .getDeclaredFields(), instance);
        }
        catch (IllegalAccessException e) {
            jlog.error("object value did not validate", e.getMessage());
            throw new KustvaktException(StatusCodes.PARAMETER_VALIDATION_ERROR,
                    "object could not be validated", instance.toString());
        }
    }


    //FIXME: currently all sets are skipped during validation (since users should not be allowed to edit those sets anyway,
    //I think we will be safe here
    @Deprecated
    private void validateStringField (Field[] fields, Object instance)
            throws KustvaktException, IllegalAccessException {
        for (Field field : fields) {
            boolean priv = false;
            if (field.getType().isAssignableFrom(String.class)) {
                if (Modifier.isPrivate(field.getModifiers())) {
                    priv = true;
                    field.setAccessible(true);
                }
                if (field.getName().equals("password")
                        | Modifier.isFinal(field.getModifiers()))
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
        protected static final int USERID_RANDOM_SIZE = 64;
        protected static final int CORPUS_RANDOM_SIZE = 48;
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
