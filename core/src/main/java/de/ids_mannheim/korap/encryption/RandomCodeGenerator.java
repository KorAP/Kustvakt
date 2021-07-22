package de.ids_mannheim.korap.encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

/**
 * Generates a random string that can be used for tokens, client id,
 * client secret, etc.
 * 
 * @author margaretha
 *
 */
@Component
public class RandomCodeGenerator {

    public static String alphanumeric = "34679bdfhmnprtFGHJLMNPRT";
    public static List<Character> charList = alphanumeric.chars()
            .mapToObj(c -> (char) c).collect(Collectors.toList());

    public final static List<String> LIMITED_CHARACTERS =
            Arrays.asList(new String[] { "3", "4", "6", "7", "9", "b", "d", "f",
                    "h", "m", "n", "p", "r", "t", "F", "G", "H", "J", "L", "M",
                    "N", "P", "R", "T" });

    private Logger log = LogManager.getLogger(RandomCodeGenerator.class);
    
    @Autowired
    public KustvaktConfiguration config;

    public static SecureRandom secureRandom;

    @PostConstruct
    public void init () throws NoSuchAlgorithmException {
        String algorithm = config.getSecureRandomAlgorithm();
        if (!algorithm.isEmpty()) {
            secureRandom = SecureRandom.getInstance(algorithm);
        }
        else {
            secureRandom = new SecureRandom();
        }
        log.info(
                "Secure random algorithm: " + secureRandom.getAlgorithm());
    }

    public String createRandomCode (KustvaktConfiguration c)
            throws KustvaktException, NoSuchAlgorithmException {
        config = c;
        init();
        return createRandomCode();
    }

    public String createRandomCode () throws KustvaktException {
        UUID randomUUID = UUID.randomUUID();
        byte[] uuidBytes = randomUUID.toString().getBytes();
        byte[] secureBytes = new byte[3];
        secureRandom.nextBytes(secureBytes);

        byte[] bytes = ArrayUtils.addAll(uuidBytes, secureBytes);

        try {
            MessageDigest md = MessageDigest
                    .getInstance(config.getMessageDigestAlgorithm());
            md.update(bytes);
            byte[] digest = md.digest();
            String code = Base64.encodeBase64URLSafeString(digest);
            md.reset();
            return code;
        }
        catch (NoSuchAlgorithmException e) {
            throw new KustvaktException(StatusCodes.INVALID_ALGORITHM,
                    config.getMessageDigestAlgorithm()
                            + "is not a valid MessageDigest algorithm");
        }
    }

    public String filterRandomCode (String code) {
        StringBuffer s = new StringBuffer();
        for (char c : code.toCharArray()) {
            if (!charList.contains(c)) {
                int n = ThreadLocalRandom.current().nextInt(0,
                        charList.size());
                c = charList.get(n);
            }
            s.append(c);
        }
        return s.toString();
    }
}
