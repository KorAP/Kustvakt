package de.ids_mannheim.korap.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author hanl
 * @date 24/03/2014
 */
public class BatchBuilder {

    private static final int SINGLE_BATCH = 1;
    private static final int SMALL_BATCH = 4;
    private static final int SMALL_MEDIUM_BATCH = 6;
    private static final int MEDIUM_BATCH = 8;
    private static final int LARGE_BATCH = 12;
    private Logger log = LoggerFactory.getLogger(BatchBuilder.class);

    private JdbcOperations operations;

    public BatchBuilder(JdbcOperations operations) {
        this.operations = operations;
    }

    public <T> List<T> selectFromIDs(String query, Collection ids, RowMapper<T> mapper) {
        List l = new ArrayList(ids);
        int size = ids.size();
        List<T> values = new ArrayList<>();
        while (size > 0) {
            int batchSize = SINGLE_BATCH;
            if (size >= LARGE_BATCH)
                batchSize = LARGE_BATCH;
            else if (size >= MEDIUM_BATCH)
                batchSize = MEDIUM_BATCH;
            else if (size >= SMALL_MEDIUM_BATCH)
                batchSize = SMALL_MEDIUM_BATCH;
            else if (size >= SMALL_BATCH)
                batchSize = SMALL_BATCH;
            size -= batchSize;
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < batchSize; i++) {
                inClause.append('?');
                inClause.append(',');
            }
            inClause.deleteCharAt(inClause.length() - 1);
            String sql = query + " (" + inClause.toString() + ");";
            Object[] args = new Object[batchSize];
            List d = new ArrayList();
            for (int idx = 0; idx < batchSize; idx++) {
                args[idx] = l.get(idx);
                d.add(idx, args[idx]);
            }
            l.removeAll(d);
            try {
                values.addAll(this.operations.query(sql, args, mapper));
            } catch (DataAccessException e) {
                log.error("Exception during database retrieval", e);
            }

        }
        return values;
    }
}
