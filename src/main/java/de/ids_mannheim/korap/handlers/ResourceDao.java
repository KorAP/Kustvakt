package de.ids_mannheim.korap.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.SqlBuilder;

/**
 * Created by hanl on 7/21/14.
 */
//todo: auditing // testing
public class ResourceDao<T extends KustvaktResource>
        implements ResourceOperationIface<T> {

    private static Logger log = LoggerFactory.getLogger(ResourceDao.class);
    protected final NamedParameterJdbcTemplate jdbcTemplate;


    public ResourceDao (PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }


    @Override
    public Class<T> type () {
        return (Class<T>) KustvaktResource.class;
    }


    // todo: testing
    @Override
    public List<T> getResources (Collection<Object> ids, User user)
            throws KustvaktException {
        String sql = "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt"
                + " on rs.id=rt.child_id WHERE rs.id IN (:ids);";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", ids);
        try {
            return (List<T>) this.jdbcTemplate.query(sql, parameters,
                    new RowMapperFactory.ResourceMapper());
        }
        catch (DataAccessException e) {
            log.error(
                    "Exception during database retrieval for ids '" + ids + "'",
                    e);
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_GET_FAILED,
                    "Exception during database retrieval for ids '" + ids,
                    ids.toString());
        }

    }


    @Override
    public int updateResource (T resource, User user) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", resource.getPersistentID());
        source.addValue("name", resource.getName());
        source.addValue("desc", resource.getDescription());
        source.addValue("data", resource.getStringData());
        final String sql = "UPDATE resource_store set name=:name, data=:data, description=:desc where persistent_id=:id;";
        try {
            return this.jdbcTemplate.update(sql, source);
        }
        catch (DataAccessException e) {
            log.error("Exception during database update for id '"
                    + resource.getPersistentID() + "'", e);
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_UPDATE_FAILED, "Exception during database update for id '"
                            + resource.getPersistentID(), resource.toString());
        }
    }


    @Override
    public int[] updateResources (List<T> resources, User user)
            throws KustvaktException {
        return new int[1];
    }


    @Override
    public <T extends KustvaktResource> T findbyId (String id, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("pid", id);
        String sql = "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt"
                + " on rs.id=rt.child_id WHERE rs.persistent_id=:pid";
        //group by rs.id;";
        try {
            return (T) this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.ResourceMapper());
        }
        catch (DataAccessException e) {
            // empty results
            return null;
        }
    }


    public KustvaktResource findbyPath (String path, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("path", path);
        String sql = "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt on rs.id=rt.child_id WHERE rt.name_path=:path;";
        try {
            return this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.ResourceMapper());
        }
        catch (DataAccessException e) {
            if (e instanceof IncorrectResultSizeDataAccessException)
                throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                        "invalid request path given!", path);
            return null;
        }
    }


    @Override
    public <T extends KustvaktResource> T findbyId (Integer id, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);
        String sql = "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt on rs.id=rt.child_id "
                + "WHERE rs.id=:id group by rs.id order by rt.depth desc;";
        try {
            return (T) this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.ResourceMapper());
        }
        catch (DataAccessException e) {
            if (e instanceof IncorrectResultSizeDataAccessException)
                throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                        "invalid request id given!", String.valueOf(id));
            return null;
        }
    }


    @Override
    public <T1 extends KustvaktResource> List<T1> findbyPartialId (String id,
            User user) throws KustvaktException {
        return null;
    }


    @Override
    public int storeResource (T resource, User user) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        KeyHolder holder = new GeneratedKeyHolder();
        // parent_id necessary so trigger can be used for tree insert!
        final String sql, parid;
        SqlBuilder b = new SqlBuilder("resource_store");
        b.insert(Attributes.NAME, Attributes.PARENT_ID,
                Attributes.PERSISTENT_ID, Attributes.DESCRIPTION,
                Attributes.CREATOR, Attributes.TYPE, Attributes.CREATED);
        b.params(
                ":name, :parent, :pid, :desc, :ow, :type, :created, :dtype, :data");

        if (resource.getParentID() == null) {
            sql = "INSERT INTO resource_store (name, parent_id, persistent_id, description, creator, type, created, data) "
                    + "VALUES (:name, :parent, :pid, :desc, :ow, :type, :created, :data);";
            parid = null;
        }
        else {
            // fixme: use trigger for consistency check!
            sql = "INSERT INTO resource_store (name, parent_id, persistent_id, description, creator, type, created, data) "
                    + "select :name, id, :pid, :desc, :ow, :type, :created, :data from resource_store where persistent_id=:parent;";
            parid = resource.getParentID();
        }

        source.addValue("name", resource.getName());
        source.addValue("pid", resource.getPersistentID());
        source.addValue("parent", parid);
        source.addValue("ow", user.getId());
        source.addValue("desc", resource.getDescription());
        source.addValue("type",
                ResourceFactory.getResourceMapping(resource.getClass()));
        source.addValue("created", System.currentTimeMillis());
        source.addValue("data", resource.getStringData());

        try {
            this.jdbcTemplate.update(sql, source, holder,
                    new String[] { "id" });
        }
        catch (DataAccessException e) {
            log.error("Exception during database store for id '"
                    + resource.getPersistentID() + "'", e);
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_INSERT_FAILED,
                    "Exception during database store for id '"
                            + resource.getPersistentID(),
                    resource.toString());
        }
        resource.setId(holder.getKey().intValue());
        return resource.getId();
    }


    @Override
    public int deleteResource (String id, User user) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);
        final String sql = "DELETE FROM resource_store WHERE persistent_id=:id;";
        try {
            return this.jdbcTemplate.update(sql, source);
        }
        catch (DataAccessException e) {
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_DELETE_FAILED, "Operation DELETE failed.",
                    id);
        }
    }


    @Override
    public int size () {
        final String sql = "SELECT COUNT(*) FROM resource_store;";
        try {
            return this.jdbcTemplate.queryForObject(sql,
                    new HashMap<String, Object>(), Integer.class);
        }
        catch (DataAccessException e) {
            return 0;
        }
    }


    @Override
    public int truncate () {
        final String sql = "DELETE FROM resource_store;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }
        catch (DataAccessException e) {
            return -1;
        }
    }

}
