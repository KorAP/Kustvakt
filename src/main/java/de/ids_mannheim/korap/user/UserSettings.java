package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.config.Attributes;

/**
 * @author hanl
 * @date 28/01/2016
 */
public class UserSettings extends Userdata {

    public UserSettings (Integer userid) {
        super(userid);
    }


    @Override
    public String[] requiredFields () {
        return new String[] {};
    }


    @Override
    public String[] defaultFields () {
        return new String[] { Attributes.DEFAULT_REL_FOUNDRY,
                Attributes.DEFAULT_POS_FOUNDRY,
                Attributes.DEFAULT_CONST_FOUNDRY,
                Attributes.DEFAULT_LEMMA_FOUNDRY, Attributes.QUERY_LANGUAGE,
                Attributes.PAGE_LENGTH };
    }
}
