package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.ResourceOperationIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * Created by hanl on 7/21/14.
 */
//todo: auditing // testing
public class ResourceDao<T extends KustvaktResource>
        implements ResourceOperationIface<T> {

    private static Logger log = KustvaktLogger.getLogger(ResourceDao.class);
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    public ResourceDao(PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }

    @Override
    public Class<T> getType() {
        return (Class<T>) KustvaktResource.class;
    }

    @Override
    public List<T> getResources(Collection<Object> ids, User user)
            throws KustvaktException {
        return null;
    }

    @Override
    public int updateResource(T resource, User user) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", resource.getPersistentID());
        source.addValue("name", resource.getName());
        source.addValue("desc", resource.getDescription());
        final String sql = "UPDATE resource_store set name=:name, description=:desc where persistent_id=:id;";
        try {
            return this.jdbcTemplate.update(sql, source);
        }catch (DataAccessException e) {
            log.error("Exception during database update for id '" + resource
                    .getPersistentID() + "'", e);
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_UPDATE_FAILED, resource.toString());
        }
    }

    @Override
    public int[] updateResources(List<T> resources, User user)
            throws KustvaktException {
        return new int[1];
    }

    @Override
    public <T extends KustvaktResource> T findbyId(String id, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("pid", id);
        String sql =
                "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt"
                        + " on rs.id=rt.child_id WHERE rs.persistent_id=:pid group by rs.id;";
        try {
            return (T) this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.ResourceMapper());
        }catch (DataAccessException e) {
            return null;
        }
    }

    public KustvaktResource findbyPath(String path, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("path", path);
        String sql = "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt on rs.id=rt.child_id WHERE rt.name_path=:path;";
        try {
            return this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.ResourceMapper());
        }catch (DataAccessException e) {
            if (e instanceof IncorrectResultSizeDataAccessException)
                throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                        "invalid request path given!", path);
            return null;
        }
    }

    @Override
    public <T extends KustvaktResource> T findbyId(Integer id, User user)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);
        String sql =
                "SELECT rs.*, rt.name_path FROM resource_store as rs inner join resource_tree as rt on rs.id=rt.child_id "
                        + "WHERE rs.id=:id group by rs.id order by rt.depth desc;";
        try {
            return (T) this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapperFactory.ResourceMapper());
        }catch (DataAccessException e) {
            return null;
        }
    }

    @Override
    public int storeResource(T resource, User user) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        KeyHolder holder = new GeneratedKeyHolder();
        // parent_id necessary so trigger can be used for tree insert!
        final String sql, parid;
        if (resource.getParentID() == null) {
            sql = "INSERT INTO resource_store (name, parent_id, persistent_id, description, creator, type, created) "
                    + "VALUES (:name, :parent, :pid, :desc, :ow, :type, :created);";
            parid = null;
        }else {
            sql = "INSERT INTO resource_store (name, parent_id, persistent_id, description, creator, type, created) "
                    + "select :name, id, :pid, :desc, :ow, :type, :created from resource_store where persistent_id=:parent;";
            parid = resource.getParentID();
        }

        source.addValue("name", resource.getName());
        source.addValue("pid", resource.getPersistentID());
        source.addValue("parent", parid);
        source.addValue("ow", resource.getOwner());
        source.addValue("desc", resource.getDescription());
        source.addValue("type",
                ResourceFactory.getResourceMapping(resource.getClass()));
        source.addValue("created",
                new Timestamp(TimeUtils.getNow().getMillis()));
        try {
            this.jdbcTemplate
                    .update(sql, source, holder, new String[] { "id" });
        }catch (DataAccessException e) {
            log.error("Exception during database store for id '" + resource
                    .getPersistentID() + "'", e);
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_INSERT_FAILED, resource.toString());
        }
        resource.setId(holder.getKey().intValue());
        return resource.getId();
    }

    @Override
    public int deleteResource(String id, User user) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);
        final String sql = "DELETE FROM resource_store WHERE persistent_id=:id;";
        try {
            return this.jdbcTemplate.update(sql, source);
        }catch (DataAccessException e) {
            e.printStackTrace();
            throw new dbException(user.getId(), "resource_store",
                    StatusCodes.DB_DELETE_FAILED, id);
        }
    }
}
