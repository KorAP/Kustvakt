package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 08/01/2016
 */
public class EsapiTest extends BeanConfigTest {

    @Test
    public void testInputPassword () throws KustvaktException {
        String pass = TestHelper.getUserCredentials()[1];
        EncryptionIface cr = helper().getContext().getEncryption();
        String spass = cr.validateEntry(pass, Attributes.PASSWORD);
        assertNotNull(spass);
        assertFalse(spass.isEmpty());
    }


    @Test
    public void testMapValidation () throws KustvaktException {
        int exp_size = KustvaktConfiguration.KUSTVAKT_USER.size();
        Map map = helper().getContext().getEncryption()
                .validateMap(KustvaktConfiguration.KUSTVAKT_USER);
        assertEquals(exp_size, map.size());
    }


    @Test
    public void testUsernameValidation () throws KustvaktException {
        String sus = helper()
                .getContext()
                .getEncryption()
                .validateEntry(
                        (String) KustvaktConfiguration.KUSTVAKT_USER
                                .get(Attributes.USERNAME),
                        Attributes.USERNAME);
        assertNotNull(sus);
        assertFalse(sus.isEmpty());
    }


    @Override
    public void initMethod () throws KustvaktException {

    }
}
