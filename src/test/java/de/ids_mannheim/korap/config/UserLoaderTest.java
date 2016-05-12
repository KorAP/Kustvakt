package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.EntityDao;
import de.ids_mannheim.korap.web.service.UserLoader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author hanl
 * @date 11/02/2016
 */
public class UserLoaderTest extends BeanConfigTest {

    @Test
    public void testUserLoader() {
        EntityDao dao = new EntityDao(helper().getContext().getPersistenceClient());

        boolean error = false;
        UserLoader l = new UserLoader();
        try {
            l.load(helper().getContext());
        }catch (KustvaktException e) {
            e.printStackTrace();
            error = true;
        }
        Assert.assertFalse(error);
        Assert.assertNotEquals("Is not supposed to be zero", 0, dao.size());
    }

    @Override
    public void initMethod() throws KustvaktException {

    }
}
