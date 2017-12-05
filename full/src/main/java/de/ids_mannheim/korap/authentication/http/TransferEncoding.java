package de.ids_mannheim.korap.authentication.http;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

/** TransferEncoding contains encoding and decoding methods for data transfer, 
 *  e.g. transfering credentials using basic Http authentication.  
 *   
 * @author margaretha
 *
 */
@Component
public class TransferEncoding {

    /** Encodes username and password using Base64.
     * 
     * @param username username
     * @param password password
     * @return
     */
    public String encodeBase64 (String username, String password) {
        String s = username + ":" + password;
        return new String(Base64.encodeBase64(s.getBytes()));
    }

    /** Decodes the given string using Base64.
     * 
     * @param encodedStr 
     * @return username and password as an array of strings.
     * @throws KustvaktException 
     */
    public String[] decodeBase64 (String encodedStr)
            throws KustvaktException {

        ParameterChecker.checkStringValue(encodedStr, "encoded string");
        String decodedStr = new String(Base64.decodeBase64(encodedStr));

        if (decodedStr.contains(":") && decodedStr.split(":").length == 2) {
            String[] strArr = decodedStr.split(":");
            if ((strArr[0] != null && !strArr[0].isEmpty())
                    && (strArr[1] != null && !strArr[1].isEmpty())) {
                return decodedStr.split(":");
            }

        }

        throw new IllegalArgumentException(
                "Unknown Base64 encoding format: " + decodedStr);
    }
}
