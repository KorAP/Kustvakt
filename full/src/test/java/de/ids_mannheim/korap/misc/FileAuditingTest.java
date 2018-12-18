package de.ids_mannheim.korap.misc;

import java.util.Date;

import org.joda.time.LocalDate;
import org.junit.Test;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.defaults.DefaultAuditing;

/**
 * @author hanl
 * @date 27/07/2015
 */
// todo: test audit commit in thread and that no concurrency issue
// arrises
public class FileAuditingTest {

    @Test
    public void testAdd () {
        AuditingIface auditor = new DefaultAuditing();
        for (int i = 0; i < 20; i++) {
            AuditRecord record = AuditRecord.serviceRecord("MichaelHanl",
                    StatusCodes.ILLEGAL_ARGUMENT, String.valueOf(i),
                    "string value");
            auditor.audit(record);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetrieval () {
        AuditingIface auditor = new DefaultAuditing();
        auditor.retrieveRecords(new LocalDate(new Date().getTime()), 10);
    }

}
