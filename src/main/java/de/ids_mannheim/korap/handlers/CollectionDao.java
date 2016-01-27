package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.VirtualCollection;
import de.ids_mannheim.korap.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author hanl
 * @date 11/01/2014
 */

@Deprecated
//todo?! cache with ehcache and put token/sentence/paragraph numbers into cache
public class CollectionDao
        implements ResourceOperationIface<VirtualCollection> {

    private static Logger log = LoggerFactory.getLogger(CollectionDao.class);
    private BatchBuilder batchBuilder;
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    public CollectionDao(PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
        this.batchBuilder = new BatchBuilder(
                this.jdbcTemplate.getJdbcOperations());
    }

    @Override
    public Class<VirtualCollection> getType() {
        return VirtualCollection.class;
    }

    // fixme: persistentid can be done, persistence is achieved by specifing a date until which documents
    // are to be included. this excludes documents that are part of the "sperreinträge"
    public <T extends KustvaktResource> T findbyId(String id, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);
        source.addValue("user", user.getId());
        final String sql = "select * from coll_store where persistent_id=:id and user_id=:user;";
        try {
            return (T) this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.CollectionMapper());
        }catch (DataAccessException e) {
            log.error("Exception during database get for id '" + id + "'", e);
            return null;
        }
    }

    public VirtualCollection findbyId(Integer id, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);
        final String sql = "select * from coll_store where id=:id";
        try {
            return this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.CollectionMapper());
        }catch (DataAccessException e) {
            log.error("Exception during database get for id '" + id + "'", e);
            return null;
        }
    }

    public int updateResource(VirtualCollection resource, User user)
            throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        np.addValue("id", resource.getPersistentID());
        np.addValue("qy", resource.getData());
        np.addValue("name", resource.getName());
        np.addValue("desc", resource.getDescription());
        final String sql = "UPDATE coll_store SET query=:qy, name=:name, description=:desc WHERE persistent_id=:id;";
        try {
            return this.jdbcTemplate.update(sql, np);
        }catch (DataAccessException e) {
            log.error("Exception during database update for id '" + resource
                    .getPersistentID() + "'", e);
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }

    public int[] updateResources(List<VirtualCollection> resources, User user)
            throws KustvaktException {
        MapSqlParameterSource[] sources = new MapSqlParameterSource[resources
                .size()];
        final String sql = "UPDATE coll_store SET query=:qy, name=:name, description=:desc WHERE persistent_id=:id;";
        int i = 0;
        for (VirtualCollection c : resources) {
            MapSqlParameterSource np = new MapSqlParameterSource();
            np.addValue("id", c.getPersistentID());
            np.addValue("qy", c.getData());
            np.addValue("name", c.getName());
            np.addValue("desc", c.getDescription());
            sources[i++] = np;
        }
        try {
            return this.jdbcTemplate.batchUpdate(sql, sources);
        }catch (DataAccessException e) {
            log.error("Exception during database update", e);
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }

    @Override
    public int storeResource(VirtualCollection resource, User user)
            throws KustvaktException {
        if (resource.getData() != null) {
            MapSqlParameterSource np = new MapSqlParameterSource();
            np.addValue("query", resource.getData());
            np.addValue("pid", resource.getPersistentID());
            np.addValue("name", resource.getName());
            np.addValue("desc", resource.getDescription());
            np.addValue("us", user.getId());
            np.addValue("cr", System.currentTimeMillis());

            final String sql =
                    "INSERT INTO coll_store (persistent_id, name, description, user_id, query, created) "
                            + "VALUES (:pid, :name, :desc, :us, :query, :cr);";
            try {
                return this.jdbcTemplate.update(sql, np);
            }catch (DataAccessException e) {
                log.error("Exception during database store for id '" + resource
                        .getPersistentID() + "'", e);
                throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
            }
        }else
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "invalid query parameter", (String) resource.getData());
    }

    public int deleteResource(String id, User user) throws KustvaktException {
        //todo: foreign key and on delete cascade does not work properly!
        MapSqlParameterSource np = new MapSqlParameterSource();
        np.addValue("id", id);
        np.addValue("user", user.getId());
        //        final String sql = "DELETE FROM coll_store cs inner join r_store rs on rs.id=cs.id WHERE rs.persistent_id=:id;";
        final String sql = "DELETE FROM coll_store where persistent_id=:id and user_id=:user;";
        try {
            return this.jdbcTemplate.update(sql, np);
        }catch (DataAccessException e) {
            log.error("Exception during database delete for id '" + id + "'",
                    e);
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }

    @Override
    public int deleteAll() throws KustvaktException {
        final String sql = "DELETE FROM coll_store;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    @Override
    public int size() throws KustvaktException {
        final String sql = "select count(*) from coll_store;";
        try {
            return this.jdbcTemplate
                    .queryForObject(sql, new HashMap<String, Object>(),
                            Integer.class);
        }catch (DataAccessException e) {
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }

    //todo: adjust to resource id input (batch operation!)
    // fixme: test
    public List<VirtualCollection> getResources(Collection<Object> resources,
            User user) throws KustvaktException {
        final String sql1 = "SELECT * from coll_store where id in";
        //        final String sql =
        //                "SELECT rs.*, rt.name_path, cs.query FROM r_store as rs inner join r_tree as rt on rs.id=rt.childID "
        //                        + "inner join coll_store as cs on cs.id=rs.id WHERE rs.id in";
        return batchBuilder.selectFromIDs(sql1, resources,
                new RowMapperFactory.CollectionMapper());
    }

}
