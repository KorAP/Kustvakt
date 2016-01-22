package de.ids_mannheim.korap.user;

import lombok.Data;
import org.apache.commons.collections.map.CaseInsensitiveMap;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hanl
 * Date: 8/14/13
 * Time: 10:32 AM
 */

// todo: set certain fields required!
@Data
public class UserDetails {

    private Integer Id;
    private Integer userID;
    private String firstName;
    private String lastName;
    // todo :should be boolean or integer?!
    private String gender;
    private String phone;
    private String institution;
    private String email;
    private String address;
    private String country;
    @Deprecated
    private boolean privateUsage;

    public UserDetails() {
        setFirstName("");
        setLastName("");
        setPhone("");
        setEmail("");
        setGender("");
        setAddress("");
        setCountry("");
        setInstitution("");
        setPrivateUsage(true);
    }

    public static UserDetails newDetailsIterator(Map<String, String> d) {
        UserDetails details = new UserDetails();
        Map<String, String> detailMap = new CaseInsensitiveMap(d);

        if (!detailMap.isEmpty()) {
            details.setFirstName(detailMap.get(Attributes.FIRSTNAME));
            details.setLastName(detailMap.get(Attributes.LASTNAME));
            details.setPhone(detailMap.get(Attributes.PHONE));
            details.setEmail(detailMap.get(Attributes.EMAIL));
            details.setGender(detailMap.get(Attributes.GENDER));
            details.setAddress(detailMap.get(Attributes.ADDRESS));
            details.setCountry(detailMap.get(Attributes.COUNTRY));
            details.setInstitution(detailMap.get(Attributes.INSTITUTION));
            details.setPrivateUsage(
                    detailMap.get(Attributes.PRIVATE_USAGE) == null ?
                            true :
                            Boolean.valueOf(
                                    detailMap.get(Attributes.PRIVATE_USAGE)));
        }
        return details;
    }

    public void updateDetails(Map<String, String> d) {
        Map<String, Object> detailMap = new CaseInsensitiveMap(d);

        if (!detailMap.isEmpty()) {
            if (detailMap.containsKey(Attributes.FIRSTNAME))
                this.setFirstName(
                        String.valueOf(detailMap.get(Attributes.FIRSTNAME)));
            if (detailMap.containsKey(Attributes.LASTNAME))
                this.setLastName(
                        String.valueOf(detailMap.get(Attributes.LASTNAME)));
            if (detailMap.containsKey(Attributes.PHONE))
                this.setPhone(String.valueOf(detailMap.get(Attributes.PHONE)));
            if (detailMap.containsKey(Attributes.EMAIL))
                this.setEmail(String.valueOf(detailMap.get(Attributes.EMAIL)));
            if (detailMap.containsKey(Attributes.GENDER))
                this.setGender(
                        String.valueOf(detailMap.get(Attributes.GENDER)));
            if (detailMap.containsKey(Attributes.ADDRESS))
                this.setAddress(
                        String.valueOf(detailMap.get(Attributes.ADDRESS)));
            if (detailMap.containsKey(Attributes.COUNTRY))
                this.setCountry(
                        String.valueOf(detailMap.get(Attributes.COUNTRY)));
            if (detailMap.containsKey(Attributes.INSTITUTION))
                this.setInstitution(
                        String.valueOf(detailMap.get(Attributes.INSTITUTION)));
            this.setPrivateUsage(Boolean.valueOf(
                    String.valueOf(detailMap.get(Attributes.PRIVATE_USAGE))));
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> details = new HashMap<>();
        // shouldnt there be a mechanism that prevents the retrieval of all information if no scopes are given?
        // and if so, are the access_tokens specific to the scopes then?
        details.put(Attributes.EMAIL, this.email);
        details.put(Attributes.FIRSTNAME, this.firstName);
        details.put(Attributes.LASTNAME, this.lastName);
        details.put(Attributes.GENDER, this.gender);
        details.put(Attributes.PHONE, this.phone);
        details.put(Attributes.INSTITUTION, this.institution);
        details.put(Attributes.ADDRESS, this.address);
        details.put(Attributes.COUNTRY, this.country);
        details.put(Attributes.PRIVATE_USAGE, this.privateUsage);

        for (Map.Entry<String, Object> pair : details.entrySet()) {
            if (pair.getValue() == null || pair.getValue().equals("null"))
                pair.setValue("");
        }
        return details;
    }

}

