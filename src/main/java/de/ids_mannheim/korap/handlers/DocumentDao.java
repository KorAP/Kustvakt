package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.ext.interfaces.ResourceOperationIface;
import de.ids_mannheim.korap.ext.resource.Document;
import de.ids_mannheim.korap.interfaces.PersistenceClient;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.BooleanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
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
        String sql = "select * from doc_store where id=:id";
        try {
            return this.jdbcTemplate
                    .query(sql, s, new ResultSetExtractor<Document>() {
                        @Override
                        public Document extractData(ResultSet rs)
                                throws SQLException, DataAccessException {
                            Document doc = new Document(
                                    rs.getString("persistentID"));
                            doc.setId(rs.getInt("id"));
                            doc.setCreated(
                                    rs.getTimestamp("created").getTime());
                            doc.setDisabled(rs.getBoolean("disabled"));
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
        String sql = "select * from doc_store where persistentID=:id";

        try {
            return this.jdbcTemplate
                    .query(sql, s, new ResultSetExtractor<Document>() {
                        @Override
                        public Document extractData(ResultSet rs)
                                throws SQLException, DataAccessException {
                            Document doc = new Document(
                                    rs.getString("persistentID"));
                            doc.setId(rs.getInt("id"));
                            doc.setCreated(
                                    rs.getTimestamp("created").getTime());
                            doc.setDisabled(rs.getBoolean("disabled"));
                            return doc;
                        }
                    });
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

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
        final String sql = "UPDATE doc_store set disabled=:dis where persistentID=:pid;";
        return this.jdbcTemplate.update(sql, source);
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
        final String sql = "select * from doc_store where (persistentID like :corpus) limit :offset, :limit";
        try {
            return this.jdbcTemplate
                    .query(sql, source, new RowMapper<Document>() {
                        @Override
                        public Document mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            Document doc = new Document(
                                    rs.getString("persistentID"));
                            doc.setId(rs.getInt("id"));
                            doc.setDisabled(rs.getBoolean("disabled"));
                            return doc;
                        }
                    });
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    public List<String> findbyCorpus(String corpus, boolean disabled)
            throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("corpus", corpus + "%");
        s.addValue("dis", BooleanUtils.getBoolean(disabled));
        String sql = "SELECT persistentID FROM doc_store WHERE (persistentID like :corpus) AND disabled=:dis;";
        try {
            return this.jdbcTemplate.queryForList(sql, s, String.class);
        }catch (DataAccessException e) {
            e.printStackTrace();
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

        String sql = "INSERT INTO doc_store (persistentID, disabled) VALUES (:id, :dis)";
        try {
            return this.jdbcTemplate.update(sql, s);
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "illegal argument given", resource.getPersistentID());
        }
    }

    @Override
    public int deleteResource(String id, User user) throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("id", id);
        String sql = "delete from doc_store where persistentID=:id;";
        try {
            return this.jdbcTemplate.update(sql, s);
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }

    }
}
