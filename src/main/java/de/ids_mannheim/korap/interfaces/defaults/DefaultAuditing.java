package de.ids_mannheim.korap.interfaces.defaults;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.interfaces.AuditingIface;
import de.ids_mannheim.korap.user.User;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;

/**
 * @author hanl
 * @date 05/06/2015
 */
public class DefaultAuditing extends AuditingIface {

    public DefaultAuditing() {

    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, DateTime day, DateTime until,
            boolean exact, int limit) {
        return null;
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, User user, int limit) {
        return null;
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(LocalDate day,
            int hitMax) {
        return null;
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(String userID,
            LocalDate start, LocalDate end, int hitMax) {
        return null;
    }

    @Override
    public void run() {
        //todo: append to logging file or other auditing file
    }
}
