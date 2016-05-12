package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class UserLoader implements BootableBeanInterface {

    @Override
    public void load(ContextHolder beans) throws KustvaktException {
        AuthenticationManagerIface manager = beans
                .getAuthenticationManager();
        manager.createUserAccount(KustvaktConfiguration.KUSTVAKT_USER,
                false);
    }

    @Override
    public Class<? extends BootableBeanInterface>[] getDependencies() {
        return new Class[0];
    }
}
