package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.interfaces.AuditingIface;
import de.ids_mannheim.korap.interfaces.PersistenceClient;
import de.ids_mannheim.korap.user.User;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author hanl
 * @date 13/01/2014
 */
public class JDBCAuditing extends AuditingIface {

    private NamedParameterJdbcTemplate template;

    public JDBCAuditing(PersistenceClient client) {
        this.template = (NamedParameterJdbcTemplate) client.getSource();
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, DateTime day, DateTime until,
            boolean dayOnly, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("limit", limit);
        p.addValue("cat", category.toString());

        String sql =
                "select * from audit_records where aud_timestamp > :today AND"
                        + " aud_timestamp < :tomorr AND aud_category=:cat limit :limit;";

        if (dayOnly) {
            LocalDate today = day.toLocalDate();
            DateTime start = today.toDateTimeAtStartOfDay(day.getZone());
            DateTime end = today.plusDays(1)
                    .toDateTimeAtStartOfDay(day.getZone());
            p.addValue("today", start.getMillis());
            p.addValue("tomorr", end.getMillis());
        }else {
            p.addValue("today", day.getMillis());
            p.addValue("tomorr", until.getMillis());
        }
        return (List<T>) this.template
                .query(sql, p, new RowMapperFactory.AuditMapper());
    }

    @Override
    public <T extends AuditRecord> List<T> retrieveRecords(
            AuditRecord.CATEGORY category, User user, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("limit", limit);
        p.addValue("us", user.getUsername());
        p.addValue("cat", category.toString());

        String sql =
                "select * from audit_records where aud_category=:cat and aud_user=:us "
                        + "order by aud_timestamp desc limit :limit;";

        return (List<T>) this.template
                .query(sql, p, new RowMapperFactory.AuditMapper());
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
    public void apply() {
        String sql;
        sql = "INSERT INTO audit_records (aud_target, aud_category, aud_user, aud_location, aud_timestamp, "
                + "aud_status, aud_field_1, aud_args) "
                + "VALUES (:target, :category, :account, :loc, :timestamp, :status, :field, :args);";
        List<AuditRecord> records = getRecordsToSave();
        SqlParameterSource[] s = new SqlParameterSource[records.size()];
        for (int i = 0; i < records.size(); i++) {
            AuditRecord rec = records.get(i);
            MapSqlParameterSource source = new MapSqlParameterSource();
            source.addValue("category", rec.getCategory().toString());
            source.addValue("account", rec.getUserid());
            source.addValue("target", rec.getTarget());
            source.addValue("loc", rec.getLoc());
            source.addValue("timestamp", new Timestamp(rec.getTimestamp()));
            source.addValue("status", rec.getStatus());
            source.addValue("field", rec.getField_1());
            source.addValue("args", rec.getArgs());
            s[i] = source;
        }
        this.template.batchUpdate(sql, s);
        records.clear();
    }

    @Override
    public void finish() {

    }

}
