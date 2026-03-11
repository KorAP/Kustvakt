package de.ids_mannheim.korap.user;

/**
 * Created by hanl on 07.06.16.
 */
public class GenericUserData extends Userdata {

    public GenericUserData () {
        super();
    }

    @Override
    public String[] requiredFields () {
        return new String[0];
    }

    @Override
    public String[] defaultFields () {
        return new String[0];
    }
}
