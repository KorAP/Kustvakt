package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.User;

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

            BeanConfiguration.getBeans().getUserDBHandler()
                    .createAccount(User.UserFactory.getDemoUser());
            manager.createUserAccount(KustvaktConfiguration.KUSTVAKT_USER,
                    false);
        }
    }

    @Override
    public int position() {
        return 0;
    }
}
