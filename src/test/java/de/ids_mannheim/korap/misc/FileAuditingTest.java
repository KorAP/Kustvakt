import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.Date;

/**
 * @author hanl
 * @date 27/07/2015
 */
//todo: test audit commit in thread and that no concurrency issue arrises
public class FileAuditingTest extends BeanConfigTest {

    @Override
    public void initMethod () throws KustvaktException {

    }


    @Test
    public void testAdd () {
        for (int i = 0; i < 20; i++) {
            AuditRecord record = AuditRecord.serviceRecord("MichaelHanl",
                    StatusCodes.ILLEGAL_ARGUMENT, String.valueOf(i),
                    "string value");
            helper().getContext().getAuditingProvider().audit(record);
        }
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testRetrieval () {
        helper().getContext().getAuditingProvider()
                .retrieveRecords(new LocalDate(new Date().getTime()), 10);
    }

}
