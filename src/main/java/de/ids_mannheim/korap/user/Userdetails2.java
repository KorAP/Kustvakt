package de.ids_mannheim.korap.user;

/**
 * @author hanl
 * @date 22/01/2016
 * persistence issue with query request
 */
public class Userdetails2 extends Userdata {

    public Userdetails2(Integer userid) {
        super(userid);
    }

    @Override
    public String[] requiredFields() {
        return new String[] { Attributes.EMAIL, Attributes.ADDRESS,
                Attributes.LASTNAME, Attributes.FIRSTNAME };
    }

}
