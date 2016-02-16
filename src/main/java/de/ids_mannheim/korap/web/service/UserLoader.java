package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class UserLoader implements BootupInterface {
    @Override
    public void load() throws KustvaktException {
        if (BeanConfiguration.hasContext()) {
            AuthenticationManagerIface manager = BeanConfiguration.getBeans()
                    .getAuthenticationManager();
            manager.createUserAccount(KustvaktConfiguration.KUSTVAKT_USER,
                    false);
        }
    }

    @Override
    public Class<? extends BootupInterface>[] getDependencies() {
        return new Class[0];
    }
}
