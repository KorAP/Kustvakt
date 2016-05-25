package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Corpus;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.security.ac.ResourceFinder;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/**
 * @author hanl
 * @date 06/02/2016
 */
public class ResourceFinderTest extends BeanConfigTest {

    @Test
    public void searchResources () throws KustvaktException {
        Set<VirtualCollection> resources = ResourceFinder
                .searchPublic(VirtualCollection.class);
        assertFalse(resources.isEmpty());
        assertEquals(3, resources.size());
    }


    @Test
    public void searchResourcesDemo () throws KustvaktException {
        Set<Corpus> resources = ResourceFinder.searchPublic(Corpus.class);
        assertFalse(resources.isEmpty());
        assertNotEquals(0, resources.size());
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
        helper().runBootInterfaces();
    }
}
