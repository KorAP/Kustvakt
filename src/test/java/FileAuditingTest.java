import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Date;

/**
 * @author hanl
 * @date 27/07/2015
 */
//todo: test audit commit in thread and that no concurrency issue arrises
public class FileAuditingTest {

    @BeforeClass
    public static void init() {
        BeanConfiguration.loadClasspathContext();
    }

    @AfterClass
    public static void finish() {
        BeanConfiguration.closeApplication();
        File f = new File("logs/default_audit.log");
        assert f.length() > 0;
    }

    @Test
    public void testAdd() {
        for (int i = 0; i < 20; i++) {
            AuditRecord record = AuditRecord
                    .serviceRecord("MichaelHanl", StatusCodes.ILLEGAL_ARGUMENT,
                            String.valueOf(i), "string value");
            BeanConfiguration.getBeans().getAuditingProvider().audit(record);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetrieval() {
        BeanConfiguration.getBeans().getAuditingProvider()
                .retrieveRecords(new LocalDate(new Date().getTime()), 10);
    }

}
