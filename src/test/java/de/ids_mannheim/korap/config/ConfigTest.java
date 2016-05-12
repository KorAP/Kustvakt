package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.service.BootableBeanInterface;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 02/09/2015
 */
public class ConfigTest extends BeanConfigTest {

    @Test
    public void testServiceInfo() {
        String version = ServiceInfo.getInfo().getVersion();
        String name = ServiceInfo.getInfo().getName();
        assertNotEquals("wrong version", "UNKNOWN", version);
        assertNotEquals("wrong name", "UNKNOWN", name);
    }

    @Test
    public void testProperties() {
        assertEquals("token layer does not match", "opennlp",
                helper().getContext().getConfiguration().getDefault_token());
        assertEquals("token expiration does not match",
                TimeUtils.convertTimeToSeconds("1D"),
                helper().getContext().getConfiguration().getLongTokenTTL());
    }

    @Test(expected = KustvaktException.class)
    public void testBeanOverrideInjection() throws KustvaktException {
        helper().getContext().getConfiguration().setPropertiesAsStream(
                ConfigTest.class.getClassLoader()
                        .getResourceAsStream("kustvakt.conf"));

        String v = "testmail_&234@ids-mannheim.de";
        helper().getContext().getEncryption()
                .validateEntry(v, Attributes.EMAIL);
    }

    @Test
    public void testBootConfigRun() {
        helper().runBootInterfaces();
        helper().setupAccount();
        assertNotNull(helper().getUser());

        Set<Class<? extends BootableBeanInterface>> set = KustvaktClassLoader
                .loadSubTypes(BootableBeanInterface.class);

        int check = set.size();
        List<String> tracker = new ArrayList<>();
        List<BootableBeanInterface> list = new ArrayList<>(set.size());
        for (Class cl : set) {
            BootableBeanInterface iface;
            try {
                iface = (BootableBeanInterface) cl.newInstance();
                list.add(iface);
            }catch (InstantiationException | IllegalAccessException e) {
                // do nothing
            }
        }

        while (!set.isEmpty()) {
            out_loop:
            for (BootableBeanInterface iface : new ArrayList<>(list)) {
                for (Class cl : iface.getDependencies()) {
                    if (set.contains(cl))
                        continue out_loop;
                }
                tracker.add(iface.getClass().getSimpleName());
                set.remove(iface.getClass());
                list.remove(iface);
            }
        }
        assertEquals(check, tracker.size());
    }

    @Test
    public void testBootConfigDependencyOrder() {
        // todo:

    }

    @Override
    public void initMethod() throws KustvaktException {

    }
}


