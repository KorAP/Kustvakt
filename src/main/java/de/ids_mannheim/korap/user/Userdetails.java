package de.ids_mannheim.korap.user;

/**
 * @author hanl
 * @date 22/01/2016
 * persistence issue with query request
 */
public class UserDetails extends Userdata {

    public UserDetails(Integer userid) {
        super(userid);
    }

    @Override
    public String[] requiredFields() {
        return new String[] { Attributes.EMAIL, Attributes.ADDRESS,
                Attributes.LASTNAME, Attributes.FIRSTNAME };
    }

    @Override
    public String[] defaultFields() {
        return new String[] { Attributes.EMAIL, Attributes.ADDRESS,
                Attributes.LASTNAME, Attributes.FIRSTNAME, Attributes.PHONE,
                Attributes.COUNTRY, Attributes.INSTITUTION, Attributes.GENDER };
    }

}
