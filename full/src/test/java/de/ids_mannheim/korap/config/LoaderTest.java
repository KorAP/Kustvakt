package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.service.BootableBeanInterface;
import de.ids_mannheim.korap.web.service.CollectionLoader;
import de.ids_mannheim.korap.web.service.PolicyLoader;
import de.ids_mannheim.korap.web.service.UserLoader;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author hanl
 * @date 12/02/2016
 */
public class LoaderTest extends BeanConfigTest {

    @Test
    @Ignore
    public void testConfigOrder () {
        System.out.println("done ...");

        List s = new ArrayList<>();
        s.add("new");
        s.add("new2");
    }


    @Override
    public void initMethod () throws KustvaktException {}


    @Test
    @Ignore
    @Deprecated
    public void runBootInterfaces () {
        Set<Class<? extends BootableBeanInterface>> set = new HashSet<>();
        set.add(CollectionLoader.class);
        set.add(PolicyLoader.class);
        set.add(UserLoader.class);

        List<BootableBeanInterface> list = new ArrayList<>(set.size());
        for (Class cl : set) {
            BootableBeanInterface iface;
            try {
                iface = (BootableBeanInterface) cl.newInstance();
                list.add(iface);
            }
            catch (InstantiationException | IllegalAccessException e) {
                // do nothing
            }
        }
        assertEquals(set.size(), list.size());
        List tracer = new ArrayList();
        System.out.println("Found boot loading interfaces: " + list);
        while (!set.isEmpty()) {
            out_loop: for (BootableBeanInterface iface : new ArrayList<>(list)) {
                try {
                    for (Class cl : iface.getDependencies()) {
                        if (set.contains(cl))
                            continue out_loop;
                    }
                    System.out.println("Running boot instructions from class "
                            + iface.getClass().getSimpleName());
                    set.remove(iface.getClass());
                    list.remove(iface);
                    iface.load(helper().getContext());
                    tracer.add(iface.getClass());
                }
                catch (KustvaktException e) {
                    // don't do anything!
                    System.out.println("An error occurred in class "
                            + iface.getClass().getSimpleName() + "!\n" + e);
                    throw new RuntimeException(
                            "Boot loading interface failed ...");
                }
            }
        }
        assertEquals(0, tracer.indexOf(UserLoader.class));
        assertNotEquals(0, tracer.indexOf(CollectionLoader.class));
        assertNotEquals(0, tracer.indexOf(PolicyLoader.class));
    }

}
