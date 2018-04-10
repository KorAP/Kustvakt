package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface EncryptionIface {

    public enum Encryption {
                @Deprecated
        SIMPLE, ESAPICYPHER, BCRYPT
    }


    /**
     * One-way hashing of String input. Used to canonicalize
     * 
     * @param input
     * @param salt
     * @return
     */
    public String secureHash (String input, String salt)
            throws KustvaktException;


    public String secureHash (String input) throws NoSuchAlgorithmException,
            UnsupportedEncodingException, KustvaktException;


    /**
     * @param plain
     * @param hash
     * @param salt
     * @return
     */
    public boolean checkHash (String plain, String hash, String salt);


    public boolean checkHash (String plain, String hash);


    /**
     * create random String to be used as authentication token
     * 
     * @return
     */
    public String createToken (boolean hash, Object ... obj);


    public String createToken ();


    /**
     * create a random Integer to be used as ID for databases
     * 
     * @return
     */
    public String createRandomNumber (Object ... obj);


    public String encodeBase ();


   // @Deprecated
    //public Map<String, Object> validateMap (Map<String, Object> map)
    //        throws KustvaktException;


    //@Deprecated
    //public String validateEntry (String input, String type)
    //        throws KustvaktException;

}
