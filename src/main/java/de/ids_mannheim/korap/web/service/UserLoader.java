package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.EntityDao;

/**
 * @author hanl
 * @date 12/01/2016
 */
public class UserLoader implements BootupInterface {
    @Override
    public void load() throws KustvaktException {
        boolean r = BeanConfiguration.hasContext();
        if (r) {
            EntityDao dao = new EntityDao(
                    BeanConfiguration.getBeans().getPersistenceClient());

            if (dao.size() > 0)
                return;

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
