package de.ids_mannheim.korap.web.service;

import java.util.HashMap;
import java.util.Map;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;

/**
 * @author hanl
 * @date 12/01/2016
 */
@Deprecated
public class UserLoader implements BootableBeanInterface {

    @Override
    public void load (ContextHolder beans) throws KustvaktException {
        AuthenticationManagerIface manager = beans.getAuthenticationManager();
        manager.createUserAccount(KustvaktConfiguration.KUSTVAKT_USER, false);
        
        // EM: Fix me
        // EM: Hack for LDAP User
        // 
        Map<String, Object> ldapUser = new HashMap<>();
        ldapUser.put(Attributes.ID, 101);
        ldapUser.put(Attributes.USERNAME, "LDAPDefaultUser");
        ldapUser.put(Attributes.PASSWORD, "unnecessary123");
        ldapUser.put(Attributes.EMAIL, "korap@ids-mannheim.de");
        ldapUser.put(Attributes.COUNTRY, "unnecessary");
        ldapUser.put(Attributes.ADDRESS, "unnecessary");
        ldapUser.put(Attributes.FIRSTNAME, "unnecessary");
        ldapUser.put(Attributes.LASTNAME, "unnecessary");
        ldapUser.put(Attributes.INSTITUTION, "IDS");
        ldapUser.put(Attributes.IS_ADMIN, "false");
        
        manager.createUserAccount(ldapUser, false);
    }


    @Override
    public Class<? extends BootableBeanInterface>[] getDependencies () {
        return new Class[0];
    }
}
