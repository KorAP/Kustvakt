package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.handlers.ResourceDao;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserSettings;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by hanl on 03.06.16.
 */
public class BeanInjectTest {

    @Test
    public void testUserBeans () {
        BeansFactory.loadClasspathContext("test-config.xml");
        Collection coll = BeansFactory.getKustvaktContext()
                .getUserDataProviders();
        assertFalse(coll.isEmpty());
        Object o = BeansFactory.getTypeFactory().getTypeInterfaceBean(coll,
                UserSettings.class);
        assertEquals(UserSettingsDao.class, AopUtils.getTargetClass(o));

        o = BeansFactory.getTypeFactory().getTypeInterfaceBean(coll,
                UserDetails.class);
        assertEquals(UserDetailsDao.class, AopUtils.getTargetClass(o));
        BeansFactory.closeApplication();
    }


    @Test
    public void testResourceBeans () {
        BeansFactory.loadClasspathContext("test-config.xml");
        Collection coll = BeansFactory.getKustvaktContext()
                .getResourceProviders();
        assertFalse(coll.isEmpty());
        Object o = BeansFactory.getTypeFactory().getTypeInterfaceBean(coll,
                Document.class);
        assertEquals(DocumentDao.class, AopUtils.getTargetClass(o));

        o = BeansFactory.getTypeFactory().getTypeInterfaceBean(coll,
                KustvaktResource.class);
        assertEquals(ResourceDao.class, AopUtils.getTargetClass(o));
        BeansFactory.closeApplication();
    }
}
