package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class UserLoader implements BootupInterface {
    @Override
    public void load() throws KustvaktException {
        if (BeanConfiguration.hasContext()) {
            BeanConfiguration.getBeans().getAuthenticationManager()
                    .createUserAccount(KustvaktConfiguration.KUSTVAKT_USER,
                            false);
        }
    }

    @Override
    public int position() {
        return 0;
    }
}
