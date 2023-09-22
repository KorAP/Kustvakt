package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import de.ids_mannheim.korap.config.ConfigLoader;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.utils.ServiceInfo;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author hanl
 * @date 02/09/2015
 */
@DisplayName("Config Test")
class ConfigTest extends SpringJerseyTest {

    @Autowired
    KustvaktConfiguration config;

    @Test
    @DisplayName("Test Config Loader")
    void testConfigLoader() {
        InputStream stream = ConfigLoader.loadConfigStream("kustvakt.conf");
        assertNotNull(stream);
    }

    @Test
    @DisplayName("Test Property Loader")
    void testPropertyLoader() throws IOException {
        Properties p = ConfigLoader.loadProperties("kustvakt.conf");
        assertNotNull(p);
    }

    @Test
    @DisplayName("Test Service Info")
    void testServiceInfo() {
        String version = ServiceInfo.getInfo().getVersion();
        String name = ServiceInfo.getInfo().getName();
        assertNotEquals("UNKNOWN", version, "wrong version");
        assertNotEquals("UNKNOWN", name, "wrong name");
    }

    @Test
    @DisplayName("Test Properties")
    void testProperties() {
        assertEquals("opennlp", config.getDefault_orthography(), "token layer does not match");
        assertEquals(TimeUtils.convertTimeToSeconds("1D"), config.getLongTokenTTL(), "token expiration does not match");
    }
}
