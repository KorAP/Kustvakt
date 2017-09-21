package de.ids_mannheim.korap.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.ResourceFinder;

/**
 * @author hanl
 * @date 06/02/2016
 */
@Deprecated
@Ignore
public class ResourceFinderTest extends BeanConfigTest {

    @Test
    public void searchResources () throws KustvaktException {
        Set<VirtualCollection> resources = ResourceFinder
                .searchPublic(VirtualCollection.class);
        assertFalse(resources.isEmpty());
        assertEquals(1, resources.size());
    }


    @Test
    public void searchResourcesDemo () throws KustvaktException {
        Set<Corpus> resources = ResourceFinder.searchPublic(Corpus.class);
        assertNotEquals(0, resources.size());
    }


    @Test
    @Deprecated
    public void testResourcesDemoFiltered () throws KustvaktException {
        Set<Corpus> resources = ResourceFinder.searchPublicFiltered(
                Corpus.class, "WPD13");
        assertNotEquals(0, resources.size());
        assertEquals(1, resources.size());

        resources = ResourceFinder.searchPublicFiltered(Corpus.class, "WPD13",
                "GOE");
        assertNotEquals(0, resources.size());
        assertEquals(2, resources.size());
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
//        helper().runBootInterfaces();
    }
}
