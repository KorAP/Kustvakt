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


    //todo: define default fields and values --> so they can never be null!
    @Override
    public String[] defaultFields() {
        return new String[0];
    }
}
