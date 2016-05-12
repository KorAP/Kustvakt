package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.web.service.CollectionLoader;
import de.ids_mannheim.korap.web.service.UserLoader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author hanl
 * @date 11/02/2016
 */
public class CollectionLoaderTest extends BeanConfigTest {

    @Test
    public void testCollectionLoader() {
        ResourceDao dao = new ResourceDao(
                helper().getContext().getPersistenceClient());

        boolean error = false;
        UserLoader u = new UserLoader();
        CollectionLoader l = new CollectionLoader();
        try {
            u.load(helper().getContext());
            l.load(helper().getContext());
        }catch (KustvaktException e) {
            error = true;
        }
        Assert.assertFalse(error);
        Assert.assertNotEquals("Is not supposed to be zero", 0, dao.size());
    }

    @Override
    public void initMethod() throws KustvaktException {

    }
}
