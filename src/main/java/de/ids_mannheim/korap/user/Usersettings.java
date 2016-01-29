package de.ids_mannheim.korap.user;

/**
 * @author hanl
 * @date 28/01/2016
 */
public class UserSettings extends Userdata {


    public UserSettings(Integer userid) {
        super(userid);
    }

    @Override
    public String[] requiredFields() {
        return new String[0];
    }

    @Override
    public String[] defaultFields() {
        return new String[0];
    }
}
