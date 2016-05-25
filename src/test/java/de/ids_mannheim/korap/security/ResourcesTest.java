package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.NotAuthorizedException;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.resources.*;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import de.ids_mannheim.korap.security.ac.ResourceHandler;
import de.ids_mannheim.korap.security.ac.SecurityManager;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 20/11/2015
 */
// todo: run functions without data to check for nullpointers!
public class ResourcesTest extends BeanConfigTest {

    private static Corpus c1;


    @Test
    public void testCreate () throws KustvaktException {
        ResourceHandler h = new ResourceHandler();
        Corpus ncorps = new Corpus("new_wiki");
        h.storeResources(helper().getUser(), ncorps);
    }


    @Test
    public void testGet () throws KustvaktException {
        DateTime beg = new DateTime();
        ResourceHandler h = new ResourceHandler();
        Corpus c = h.findbyStrId(c1.getPersistentID(), helper().getUser(),
                Corpus.class);
        float end = TimeUtils.floating(beg, new DateTime());
        System.out.println("END ----------------- : " + end);
        assertNotNull(c);
    }


    @Test(expected = KustvaktException.class)
    public void testGetthrowsUnauthorizedException () throws KustvaktException {
        DateTime beg = new DateTime();
        ResourceHandler h = new ResourceHandler();
        Corpus c = h.findbyStrId(c1.getPersistentID(),
                User.UserFactory.getDemoUser(), Corpus.class);
        float end = TimeUtils.floating(beg, new DateTime());
        System.out.println("END ----------------- : " + end);
        assertNotNull(c);
    }


    // in case of null, should not return nullpointer!
    @Test(expected = KustvaktException.class)
    @Ignore
    public void testCollectionGet () throws KustvaktException {
        //todo: do use test user!
        User user = User.UserFactory
                .toUser(KustvaktConfiguration.KUSTVAKT_USER);
        EntityHandlerIface ice = helper()
                .getBean(ContextHolder.KUSTVAKT_USERDB);
        User test = ice.getAccount(user.getUsername());
        assertNotNull(test);
        Set<KustvaktResource> resources = ResourceFinder.search(user,
                ResourceFactory.getResourceClass("collection"));

        assertFalse(resources.isEmpty());
        KustvaktResource r = (KustvaktResource) resources.toArray()[0];

        assertNotNull(r);
        ResourceHandler h = new ResourceHandler();
        h.findbyStrId(r.getPersistentID(), user, VirtualCollection.class);
    }


    // securitymanager does not allow for anonymous retrieval, only resourcefinder!
    @Test(expected = NotAuthorizedException.class)
    public void getResource () throws KustvaktException {
        User user = User.UserFactory.getDemoUser();
        SecurityManager m = SecurityManager.findbyId(2, user,
                Permissions.Permission.READ);
        m.getResource();
    }


    @Test
    public void getDemoResources () throws KustvaktException {
        Set s = ResourceFinder.searchPublic(Corpus.class);
        assertEquals(2, s.size());
        s = ResourceFinder.searchPublic(Foundry.class);
        assertEquals(1, s.size());
    }


    @Test
    public void getDemoResourceFiltered () throws KustvaktException {
        Set s = ResourceFinder.searchPublicFiltered(Corpus.class, "WPD");
        assertEquals(1, s.size());
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
        c1 = new Corpus("WPD_test");
        helper().runBootInterfaces();
        helper().setupResource(c1);
    }
}
