package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.service.BootableBeanInterface;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 02/09/2015
 */
public class ConfigTest extends BeanConfigTest {


    @Test
    public void testConfigLoader () {
        InputStream stream = ConfigLoader.loadConfigStream("kustvakt.conf");
        assertNotNull(stream);
    }


    @Test
    public void testPropertyLoader () throws IOException {
        Properties p = ConfigLoader.loadProperties("kustvakt.conf");
        assertNotNull(p);
    }


    @Test
    public void testAdminHash () throws IOException, KustvaktException,
            NoSuchAlgorithmException {
        AdminSetup setup = AdminSetup.getInstance();
        String hash = setup.getHash();
        File f = new File("./admin_token");
        FileInputStream stream = new FileInputStream(f);
        String token = IOUtil.toString(stream);
        assertNotEquals("", hash);
        assertNotEquals("", token);
        EncryptionIface crypto = helper().getContext().getEncryption();
        assertTrue(crypto.checkHash(token, hash));
    }


    @Test
    public void testServiceInfo () {
        String version = ServiceInfo.getInfo().getVersion();
        String name = ServiceInfo.getInfo().getName();
        assertNotEquals("wrong version", "UNKNOWN", version);
        assertNotEquals("wrong name", "UNKNOWN", name);
    }


    @Test
    public void testProperties () {
        assertEquals("token layer does not match", "opennlp", helper()
                .getContext().getConfiguration().getDefault_token());
        assertEquals("token expiration does not match",
                TimeUtils.convertTimeToSeconds("1D"), helper().getContext()
                        .getConfiguration().getLongTokenTTL());
    }


    @Test(expected = KustvaktException.class)
    @Ignore
    public void testBeanOverrideInjection () throws KustvaktException {
        helper().getContext()
                .getConfiguration()
                .setPropertiesAsStream(
                        ConfigTest.class.getClassLoader().getResourceAsStream(
                                "kustvakt.conf"));
    }


    @Test
    public void testBootConfigRun () throws KustvaktException {
//        helper().runBootInterfaces();
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
            }
            catch (InstantiationException | IllegalAccessException e) {
                // do nothing
            }
        }

        while (!set.isEmpty()) {
            out_loop: for (BootableBeanInterface iface : new ArrayList<>(list)) {
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

    // todo:
    @Test
    @Ignore
    public void testKustvaktValueValidation() {
        Map m = KustvaktConfiguration.KUSTVAKT_USER;
        EncryptionIface crypto = helper().getContext().getEncryption();


    }


    @Test
    public void testBootConfigDependencyOrder () {
        // todo:

    }

    @Override
    public void initMethod () throws KustvaktException {

    }
}
