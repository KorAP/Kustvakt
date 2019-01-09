package de.ids_mannheim.korap.oauth2.openid.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import de.ids_mannheim.korap.config.FullConfiguration;

/** JWK services for generating key pair and key set.
 * 
 * @author margaretha
 *
 */
@Service
public class JWKService {

    @Autowired
    private FullConfiguration config;

    public static void main (String[] args)
            throws NoSuchAlgorithmException, IOException {
        generateJWK();
    }

    public static void generateJWK ()
            throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        // Convert to JWK format
        JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString()).build();

        // write private key
        JSONObject json = new JSONObject(jwk.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream("kustvakt_rsa.key"));
        writer.write(json.toString(2));
        writer.flush();
        writer.close();

        JWK publicJWK = jwk.toPublicJWK();
        JWKSet jwkSet = new JWKSet(publicJWK);
        json = new JSONObject(jwkSet.toString());
        // write public key
        writer = new OutputStreamWriter(
                new FileOutputStream("kustvakt_rsa_public.key"));
        writer.write(json.toString(2));
        writer.flush();
        writer.close();
    }

    /**
     * Generates indented JSON string representation of kustvakt
     * public keys
     * 
     * @return json string of kustvakt public keys
     * 
     * @see RFC 8017 regarding RSA specifications
     * @see RFC 7517 regarding JWK (Json Web Key) and JWK Set
     * 
     */
    public String generatePublicKeySetJson () {
        JWKSet jwkSet = config.getPublicKeySet();
        JSONObject json = new JSONObject(jwkSet.toString());
        return json.toString(2);
    }
}
