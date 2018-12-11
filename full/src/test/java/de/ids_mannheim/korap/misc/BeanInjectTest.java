package de.ids_mannheim.korap.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collection;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;

import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.handlers.UserDetailsDao;
import de.ids_mannheim.korap.handlers.UserSettingsDao;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserSettings;

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

}
