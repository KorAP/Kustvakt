package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.ac.PolicyDao;
import de.ids_mannheim.korap.web.service.CollectionLoader;
import de.ids_mannheim.korap.web.service.PolicyLoader;
import de.ids_mannheim.korap.web.service.UserLoader;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Ignore;

/**
 * @author hanl
 * @date 11/02/2016
 */
@Deprecated
public class PolicyLoaderTest extends BeanConfigTest {

    @Test
    @Ignore
    public void testPolicyLoader () {
        boolean error = false;
        UserLoader u = new UserLoader();
        CollectionLoader c = new CollectionLoader();
        PolicyLoader l = new PolicyLoader();
        try {
            u.load(helper().getContext());
            c.load(helper().getContext());
            l.load(helper().getContext());
        }
        catch (KustvaktException e) {
            e.printStackTrace();
            error = true;
        }
        assertFalse(error);
        PolicyDao dao = new PolicyDao(helper().getContext()
                .getPersistenceClient());
        assertNotEquals("Is not supposed to be zero", 0, dao.size());
    }


    @Override
    public void initMethod () throws KustvaktException {

    }
}
