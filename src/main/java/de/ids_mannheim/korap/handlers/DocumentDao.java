package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.BooleanUtils;
import edu.emory.mathcs.backport.java.util.Collections;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author hanl
 * @date 05/11/2014
 */
// todo: testing!
// todo: error handling
public class DocumentDao implements ResourceOperationIface<Document> {

    private NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentDao(PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }

    @Override
    public Class<Document> getType() {
        return Document.class;
    }

    @Override
    public Document findbyId(Integer id, User user) throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("id", id);
        String sql = "select id, persistent_id, disabled, strftime('%s', created) as created from doc_store where id=:id";
        try {
            return this.jdbcTemplate
                    .queryForObject(sql, s, new RowMapper<Document>() {
                        @Override
                        public Document mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            Document doc = null;
                            if (!rs.isClosed()) {
                                String s = rs.getString("persistent_id");
                                System.out.println("VALUE IS " + s);
                                doc = new Document(
                                        rs.getString("persistent_id"));
                                doc.setId(rs.getInt("id"));
                                doc.setCreated(
                                        rs.getTimestamp("created").getTime());
                                doc.setDisabled(rs.getBoolean("disabled"));
                            }

                            return doc;
                        }
                    });
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    // document id, consisting of corpus sigle, substring key and document number
    @Override
    public Document findbyId(String id, User user) throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("id", id);
        //        strftime('%s', created) as created
        String sql = "select id, persistent_id, disabled, created from doc_store where persistent_id=:id;";
        try {
            return this.jdbcTemplate
                    .queryForObject(sql, s, new RowMapper<Document>() {
                        @Override
                        public Document mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            Document doc = null;
                            if (!rs.isClosed()) {
                                doc = new Document(
                                        rs.getString("persistent_id"));
                                doc.setId(rs.getInt("id"));
                                doc.setCreated(
                                        rs.getTimestamp("created").getTime());
                                doc.setDisabled(rs.getBoolean("disabled"));
                            }

                            return doc;
                        }
                    });
        }catch (EmptyResultDataAccessException em) {
            return null;
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    //todo:
    @Override
    public List<Document> getResources(Collection<Object> ids, User user)
            throws KustvaktException {
        return null;
    }

    @Override
    public int updateResource(Document document, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("pid", document.getPersistentID());
        source.addValue("dis", BooleanUtils.getBoolean(document.isDisabled()));
        final String sql = "UPDATE doc_store set disabled=:dis where persistent_id=:pid;";
        try {
            return this.jdbcTemplate.update(sql, source);
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    @Override
    public int[] updateResources(List<Document> resources, User user)
            throws KustvaktException {
        return new int[0];
    }

    public List<Document> findbyCorpus(String corpus, int offset, int index)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("corpus", corpus + "%");
        source.addValue("offset", (offset * index));
        source.addValue("limit", offset);
        //strftime('%s', created) as
        final String sql = "select id, persistent_id, disabled, created from doc_store where (persistent_id like :corpus) limit :offset, :limit";
        try {
            return this.jdbcTemplate
                    .query(sql, source, new RowMapper<Document>() {
                        @Override
                        public Document mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            // todo: test on empty/closed resultset!
                            if (!rs.isClosed()) {
                                Document doc = new Document(
                                        rs.getString("persistent_id"));
                                doc.setId(rs.getInt("id"));
                                doc.setCreated(
                                        rs.getTimestamp("created").getTime());
                                doc.setDisabled(rs.getBoolean("disabled"));
                                return doc;
                            }
                            return null;
                        }
                    });
        }catch (EmptyResultDataAccessException em) {
            em.printStackTrace();
            return Collections.emptyList();
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    public List<String> findbyCorpus(String corpus, boolean disabled)
            throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("corpus", corpus + "%");
        s.addValue("dis", BooleanUtils.getBoolean(disabled));
        String sql = "SELECT persistent_id FROM doc_store WHERE (persistent_id like :corpus) AND disabled=:dis;";
        try {
            return this.jdbcTemplate.queryForList(sql, s, String.class);
        }catch (EmptyResultDataAccessException em) {
            em.printStackTrace();
            return Collections.emptyList();
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    // parent is disabled here
    @Override
    public int storeResource(Document resource, User user)
            throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("id", resource.getPersistentID());
        s.addValue("corpus", resource.getCorpus());
        s.addValue("dis", BooleanUtils.getBoolean(resource.isDisabled()));

        String sql = "INSERT INTO doc_store (persistent_id, disabled) VALUES (:id, :dis)";
        try {
            return this.jdbcTemplate.update(sql, s);
        }catch (DataAccessException e) {
            e.printStackTrace();
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "illegal argument given", resource.getPersistentID());
        }
    }

    @Override
    public int deleteResource(String id, User user) throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("id", id);
        String sql = "delete from doc_store where persistent_id=:id;";
        try {
            return this.jdbcTemplate.update(sql, s);
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }

    }

    @Override
    public int deleteAll() throws KustvaktException {
        String sql = "delete from doc_store;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    @Override
    public int size() throws KustvaktException {
        return -1;
    }
}
