package de.ids_mannheim.korap.user;

/**
 * @author hanl
 * @date 28/01/2016
 */
public class UserSettings2 extends Userdata {


    public UserSettings2(Integer userid) {
        super(userid);
    }

    @Override
    public String[] requiredFields() {
        return new String[0];
    }
}
