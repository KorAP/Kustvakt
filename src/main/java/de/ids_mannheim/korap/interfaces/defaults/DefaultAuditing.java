package de.ids_mannheim.korap.interfaces.defaults;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.interfaces.AuditingIface;
import de.ids_mannheim.korap.user.User;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author hanl
 * @date 05/06/2015
 */
public class DefaultAuditing extends AuditingIface {

    private FileOutputStream stream;

    public DefaultAuditing() {
        try {
            File f = new File("logs");
            f.mkdirs();
            stream = new FileOutputStream(new File(f, "audit.log"));
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, DateTime day, DateTime until,
            boolean exact, int limit) {
        throw new UnsupportedOperationException("operation not supported!");
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, User user, int limit) {
        throw new UnsupportedOperationException("operation not supported!");
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(LocalDate day,
            int hitMax) {
        throw new UnsupportedOperationException("operation not supported!");
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(String userID,
            LocalDate start, LocalDate end, int hitMax) {
        throw new UnsupportedOperationException("operation not supported!");
    }

    @Override
    public void run() {
        try {
            for (AuditRecord r : getRecordsToSave())
                stream.write((r.toString() + "\n").getBytes());
            stream.flush();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
