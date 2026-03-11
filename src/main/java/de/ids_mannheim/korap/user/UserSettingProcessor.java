package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.config.Attributes;

/**
 * @author hanl, margaretha
 * @date 28/01/2016
 */
public class UserSettingProcessor extends Userdata {
	public UserSettingProcessor () {
        super();
    }
	
	public UserSettingProcessor (String username) {
        super(username);
    }

    // EM: added
    public UserSettingProcessor (String username,String data) {
        super(username, data);
    }

    @Override
    public String[] requiredFields () {
        return new String[] {};
    }

    @Override
    public String[] defaultFields () {
        return new String[] { Attributes.DEFAULT_FOUNDRY_RELATION,
                Attributes.DEFAULT_FOUNDRY_POS,
                Attributes.DEFAULT_FOUNDRY_CONSTITUENT,
                Attributes.DEFAULT_FOUNDRY_LEMMA, Attributes.QUERY_LANGUAGE,
                Attributes.PAGE_LENGTH };
    }
}
