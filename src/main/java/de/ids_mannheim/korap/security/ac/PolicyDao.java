package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.PolicyHandlerIface;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.security.Parameter;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.BooleanUtils;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author hanl
 * @date 14/01/2014
 */
// todo: transactions and exception management
public class PolicyDao implements PolicyHandlerIface {

    private static final Logger jlog = LoggerFactory.getLogger(PolicyDao.class);

    private NamedParameterJdbcTemplate jdbcTemplate;


    public PolicyDao (PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }


    /**
     * @param policy
     * @param user
     * @return int to indicate the rows updated/inserted
     * @throws KustvaktException
     */
    // fixme: better way of dealing with this?
    // fixme: enable needs to be set specifically for mysql db
    @Override
    public int createPolicy (SecurityPolicy policy, User user)
            throws KustvaktException {
        String sql = "INSERT INTO policy_store (target_id, creator, created, posix, enable, expire, iprange)"
                + " SELECT id, :creator, :cr, :posix, :en, :exp, :ip FROM resource_store WHERE persistent_id=:target;";

        if (policy.getTarget() == null)
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.MISSING_POLICY_TARGET, policy.toString());

        if (policy.getConditions().isEmpty())
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.MISSING_POLICY_CONDITIONS);

        if (policy.getPermissionByte() == 0)
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.MISSING_POLICY_PERMISSION);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource np = new MapSqlParameterSource();
        np.addValue("target", policy.getTarget());
        if (policy.getContext().getEnd() != 0L)
            np.addValue("exp", policy.getContext().getEnd());
        else
            np.addValue("exp", null);
        np.addValue("en", policy.getContext().getStart());
        np.addValue("posix", policy.getPermissionByte());
        np.addValue("cr", new Timestamp(TimeUtils.getNow().getMillis()));
        np.addValue("creator", user.getId());
        np.addValue("ip", policy.getContext().getIpmask());

        try {
            mapConditionsToUsers(policy, user);
            this.jdbcTemplate.update(sql, np, keyHolder, new String[] { "id" });
            policy.setID(keyHolder.getKey().intValue());
            this.mapConstraints(policy);
            return policy.getID();
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            jlog.error(
                    "Operation (INSERT) not possible for '{}' for user '{}'",
                    policy.toString(), user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_INSERT_FAILED, policy.toString());
        }
    }


    /**
     * should also include a remove operation, so removed policy
     * constraints
     * 
     * @param policy
     * @return
     * @throws KustvaktException
     */
    // benchmark this!
    @Override
    public void mapConstraints (SecurityPolicy policy) throws KustvaktException {
        final String cond = "INSERT INTO group_ref (group_id, policy_id) VALUES (:group, :policyID);";
        final String remove = "DELETE FROM group_ref WHERE group_id=:group and policy_id=:policyID;";
        try {
            List<PolicyCondition> conditions = policy.getConditions();
            int idx = 0;
            if (!policy.getRemoved().isEmpty()) {
                MapSqlParameterSource[] sources_removed = new MapSqlParameterSource[policy
                        .getRemoved().size()];
                for (Integer toremove : policy.getRemoved()) {
                    MapSqlParameterSource source = new MapSqlParameterSource();
                    source.addValue("group", conditions.get(toremove)
                            .getSpecifier());
                    source.addValue("policyID", policy.getID());
                    sources_removed[idx++] = source;
                }
                this.jdbcTemplate.batchUpdate(remove, sources_removed);
            }

            // todo: naming convention!
            if (!policy.getAdded().isEmpty()) {
                idx = 0;
                MapSqlParameterSource[] sources = new MapSqlParameterSource[policy
                        .getAdded().size()];
                for (Integer add : policy.getAdded()) {
                    MapSqlParameterSource source = new MapSqlParameterSource();
                    source.addValue("group", conditions.get(add).getSpecifier());
                    source.addValue("policyID", policy.getID());
                    sources[idx++] = source;
                }
                this.jdbcTemplate.batchUpdate(cond, sources);
            }
            policy.clear();
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            jlog.error(
                    "Operation (MAPPING POLICY CONDITIONS) not possible for '{}' for user '{}'",
                    policy.toString(), policy.getCreator());
            // throwing an error here is not recommended
            //            throw new dbException(policy.getCreator(), "policy_store",
            //                    StatusCodes.DB_INSERT_FAILED, policy.toString());
        }
    }


    // todo: check transactional behaviour! --> rollback
    private void mapConditionsToUsers (SecurityPolicy policy, User user)
            throws KustvaktException {
        for (PolicyCondition cond : policy.getConditions()) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue("name", cond.getSpecifier());
            param.addValue("userid", user.getId());

            try {
                final Integer[] results = new Integer[2];
                jdbcTemplate
                        .query("SELECT COUNT(*) as total, (select count(*) from group_users where user_id=:userid and "
                                + "group_id=:name) as users FROM group_store WHERE name=:name",
                                param, new RowCallbackHandler() {
                                    @Override
                                    public void processRow (ResultSet rs)
                                            throws SQLException {
                                        results[0] = rs.getInt("total");
                                        results[1] = rs.getInt("users");
                                    }
                                });

                boolean admin = false;
                if (results[0] == 0) {
                    admin = true;
                    this.createCondition(cond, user);
                }
                if (results[1] == 0)
                    this.addToCondition(Arrays.asList(user.getUsername()),
                            cond, admin);
            }
            catch (DataAccessException e) {
                jlog.error(
                        "Operation (SELECT) not possible for '{}' for user '{}'",
                        policy.getTarget(), user.getId());
                throw new dbException(user.getId(), "policy_store",
                        StatusCodes.DB_GET_FAILED, policy.toString());
            }
        }
    }


    // fixme: does not compare permissions. parent can still disregard policy because of missing permisssions
    @Override
    public List<SecurityPolicy>[] getPolicies (Integer target, final User user,
            Byte perm) {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("target", target);
        param.addValue("userid", user.getId());
        param.addValue("perm", perm);
        param.addValue("en", new Timestamp(TimeUtils.getNow().getMillis()));

        String sql_new = "select pv.*, pv.perm & :perm as allowed, rh.depth, (select max(depth) from resource_tree \n"
                + "where child_id=rh.child_id) as max_depth from policy_view as pv "
                + "inner join resource_tree as rh on rh.parent_id=pv.id "
                + "where rh.child_id=:target and pv.enable <= :en and (pv.expire > :en or pv.expire is NULL) and "
                + "(pv.group_id='self' or pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid)) and "
                + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) = "
                + "(select sum(distinct res.depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id where (pos.group_id in (select g.group_id from group_users as g "
                + "where g.user_id=:userid) or pos.group_id='self') and res.child_id=rh.child_id group by child_id);";

        try {
            return this.jdbcTemplate.query(sql_new, param,
                    new ResultSetExtractor<List<SecurityPolicy>[]>() {

                        @Override
                        public List<SecurityPolicy>[] extractData (ResultSet rs)
                                throws SQLException, DataAccessException {
                            return SecurityRowMappers.mapResourcePolicies(rs);
                        }
                    });
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Permission Denied for policy retrieval for '{}' for user '{}'",
                    target, user.getId());
            return new List[2];
        }
    }


    // without root policies, since these are policies from different resources!
    @Override
    public List<SecurityPolicy> getPolicies (PolicyCondition condition,
            Class<? extends KustvaktResource> clazz, Byte perm) {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("cond", condition.getSpecifier());
        param.addValue("perm", perm);
        param.addValue("type", ResourceFactory.getResourceMapping(clazz));
        param.addValue("en", new Timestamp(TimeUtils.getNow().getMillis()));
        String sql_new = "select pv.*, pv.perm & :perm as allowed, "
                + "rh.depth, (select max(depth) from resource_tree "
                + "where child_id=rh.child_id) as max_depth from policy_view as pv "
                + "inner join resource_tree as rh on rh.parent_id=pv.id "
                + "where "
                + "pv.enable <= :en and (pv.expire > :en or pv.expire is NULL) and "
                + "pv.group_id=:cond and pv.type=:type and "
                + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) = "
                + "(select sum(distinct res.depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id where (pos.group_id=:cond)"
                + " and res.child_id=rh.child_id group by child_id)";

        try {
            return this.jdbcTemplate.query(sql_new, param,
                    new ResultSetExtractor<List<SecurityPolicy>>() {

                        @Override
                        public List<SecurityPolicy> extractData (ResultSet rs)
                                throws SQLException, DataAccessException {
                            return SecurityRowMappers.mapConditionPolicies(rs);
                        }
                    });
        }
        catch (DataAccessException e) {
            jlog.error("Permission Denied: policy retrieval for '{}'",
                    condition.getSpecifier());
            return Collections.emptyList();
        }
    }


    @Override
    public List<SecurityPolicy>[] getPolicies (String target, final User user,
            Byte perm) {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("target", target);
        param.addValue("userid", user.getId());
        param.addValue("perm", perm);
        param.addValue("en", new Timestamp(TimeUtils.getNow().getMillis()));

        String sql_new = "select pv.*, pv.perm & :perm as allowed, "
                + "rh.depth, (select max(depth) from resource_tree "
                + "where child_id=rh.child_id) as max_depth from policy_view as pv "
                + "inner join resource_tree as rh on rh.parent_id=pv.id "
                + "where rh.child_id=(select id from resource_store where persistent_id=:target) and "
                + "pv.enable <= :en and (pv.expire > :en or pv.expire is NULL) and "
                + "(pv.group_id='self' or pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid)) and "
                + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) = "
                + "(select sum(distinct res.depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id where (pos.group_id in (select g.group_id from group_users "
                + "as g where g.user_id=:userid) or pos.group_id='self') and res.child_id=rh.child_id group by child_id)";
        try {
            return this.jdbcTemplate.query(sql_new, param,
                    new ResultSetExtractor<List<SecurityPolicy>[]>() {

                        @Override
                        public List<SecurityPolicy>[] extractData (ResultSet rs)
                                throws SQLException, DataAccessException {
                            List<SecurityPolicy>[] pol = SecurityRowMappers
                                    .mapResourcePolicies(rs);
                            return pol;
                        }
                    });
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Permission Denied: policy retrieval for '{}' for user '{}'",
                    target, user.getId());
            return new List[2];
        }
    }


    @Override
    public List<SecurityPolicy>[] findPolicies (String path, final User user,
            Byte perm) {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("path", StringUtils.buildSQLRegex(path));
        param.addValue("userid", user.getId());
        param.addValue("perm", perm);
        param.addValue("en", new Timestamp(TimeUtils.getNow().getMillis()));

        String sql_new = "select pv.*, pv.perm & :perm as allowed, "
                + "rh.depth, (select max(depth) from resource_tree "
                + "where child_id=rh.child_id) as max_depth from policy_view as pv "
                + "inner join resource_tree as rh on rh.parent_id=pv.id "
                + "where rt.name_path regexp :path and "
                + "pv.enable <= :en and (pv.expire > :en or pv.expire is NULL) and "
                + "(pv.group_id='self' or pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid)) and "
                + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) = "
                + "(select sum(distinct res.depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id where (pos.group_id in (select g.group_id from group_users "
                + "as g where g.user_id=:userid) or pos.group_id='self') and res.child_id=rh.child_id group by child_id)";

        try {
            return this.jdbcTemplate.query(sql_new, param,
                    new ResultSetExtractor<List<SecurityPolicy>[]>() {

                        @Override
                        public List<SecurityPolicy>[] extractData (ResultSet rs)
                                throws SQLException, DataAccessException {
                            return SecurityRowMappers.mapResourcePolicies(rs);
                        }
                    });
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Permission Denied for retrieval for resource id '{}' for user '{}'",
                    path, user.getId());
            return new List[2];
        }
    }


    /**
     * @param path
     *            if set searches in path where the child element
     *            equals name. Also applicable for root resources!
     * @param user
     * @param clazz
     * @return
     */
    //todo: not working yet!
    // todo: does not concern itsself with location matching, ever!
    @Override
    public List<KustvaktResource.Container> getDescending (String path,
            final User user, Byte b,
            final Class<? extends KustvaktResource> clazz)
            throws KustvaktException {
        final MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("userid", user.getId());
        param.addValue("type", ResourceFactory.getResourceMapping(clazz));
        param.addValue("part", "%" + path);
        param.addValue("perm", b);

        String sql;
        if (path != null && !path.isEmpty()) {
            sql = "select pv.*, pv.perm & :perm as allowed, rh.depth, rh.name_path, (select max(depth) from resource_tree \n"
                    + "where child_id=rh.child_id) as max_depth from policy_view as pv "
                    + "inner join resource_tree as rh on rh.child_id=pv.id "
                    + "where pv.type=:type and (rh.name_path like :part) and ((pv.creator=:userid and pv.group_id='self') or "
                    + "(pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid) and "
                    + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) = "
                    + "(select sum(distinct depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id "
                    + "where pos.group_id in (select g.group_id from group_users as g where g.user_id=:userid) "
                    + "and res.child_id=rh.child_id group by child_id))) "
                    + "group by pv.pid, pv.id having count(distinct pv.group_id) = "
                    + "((select count(co.group_id) from group_ref as co where co.policy_id=pv.pid) or "
                    + "(select 1 from policy_view as cp2 where cp2.group_id='self' and cp2.id=pv.id)) "
                    + "order by rh.depth asc, pv.id desc;";
        }
        else {
            sql = "select pv.*, pv.perm & :perm as allowed, rh.depth, rh.name_path, (select max(depth) from resource_tree \n"
                    + "where child_id=rh.child_id) as max_depth from policy_view as pv "
                    + "inner join resource_tree as rh on rh.child_id=pv.id "
                    + "where pv.type=:type and ((pv.creator=:userid and pv.group_id='self') or "
                    + "(pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid) and "
                    + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) = "
                    + "(select sum(distinct depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id "
                    + "where pos.group_id in (select g.group_id from group_users as g where g.user_id=:userid) "
                    + "and res.child_id=rh.child_id group by child_id))) "
                    + "group by pv.pid, pv.id having count(distinct pv.group_id) = "
                    + "((select count(co.group_id) from group_ref as co where co.policy_id=pv.pid) or "
                    + "(select 1 from policy_view as cp2 where cp2.group_id='self' and cp2.id=pv.id)) "
                    + "order by rh.depth asc, pv.id desc;";
        }
        try {
            return this.jdbcTemplate.query(sql, param,
                    new SecurityRowMappers.HierarchicalResultExtractor());
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Permission Denied for retrieval for path '{}' for user '{}'",
                    path, user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_GET_FAILED, path, clazz.toString());
        }
    }


    @Override
    public List<KustvaktResource.Container> getAscending (String name,
            User user, Byte b, Class<? extends KustvaktResource> clazz)
            throws KustvaktException {
        final MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("userid", user.getId());
        param.addValue("type", ResourceFactory.getResourceMapping(clazz));
        param.addValue("part", "%" + name);
        param.addValue("perm", b);

        String sql;
        if (name != null && !name.isEmpty()) {
            sql = "select pv.*, pv.perm & :perm as allowed, rh.depth, rh.name_path,\n"
                    + "(select max(depth) from resource_tree \n"
                    + "where child_id=rh.child_id) as max_depth from policy_view as pv\n"
                    + "inner join resource_tree as rh on rh.child_id=pv.id\n"
                    + "where pv.id in (select rt.parent_id from resource_tree as rt inner join resource_store rs on rs.id=rt.child_id\n"
                    + "where rs.type=:type and rt.name_path like :part) and ((pv.creator=:userid and pv.group_id='self') or\n"
                    + "(pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid) and\n"
                    + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) =\n"
                    + "(select sum(distinct depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.id\n"
                    + "where pos.group_id in (select g.group_id from group_users as g where g.user_id=:userid)\n"
                    + "and res.child_id=rh.child_id group by child_id)))\n"
                    + "group by pv.pid, pv.id having count(distinct pv.group_id) = \n"
                    + "case when pv.creator=:userid then 1 else (select count(distinct co.group_id) "
                    + "from group_ref as co where co.policy_id=pv.pid) end order by rh.depth desc, pv.id desc;";
        }
        else {
            sql = "select pv.*, pv.perm & :perm as allowed, rh.depth, rh.name_path,\n"
                    + "(select max(depth) from resource_tree \n"
                    + "where child_id=rh.child_id) as max_depth from policy_view as pv\n"
                    + "inner join resource_tree as rh on rh.child_id=pv.id\n"
                    + "where pv.id in (select rt.parent_id from resource_tree as rt inner join resource_store rs on rs.id=rt.child_id\n"
                    + "where rs.type=:type) and ((pv.creator=:userid and pv.group_id='self') or\n"
                    + "(pv.group_id in (select g.group_id from group_users as g where g.user_id=:userid) and\n"
                    + "(select sum(distinct depth) from resource_tree where child_id=rh.child_id) =\n"
                    + "(select sum(distinct depth) from policy_view as pos inner join resource_tree as res on res.parent_id=pos.target_id\n"
                    + "where pos.group_id in (select g.group_id from group_users as g where g.user_id=:userid)\n"
                    + "and res.child_id=rh.child_id group by child_id)))\n"
                    + "group by pv.pid, pv.id having count(distinct pv.group_id) = \n"
                    + "case when pv.creator=:userid then 1 else (select count(distinct co.group_id) "
                    + "from group_ref as co where co.policy_id=pv.pid) end order by rh.depth desc, pv.id desc;";
        }
        try {
            return this.jdbcTemplate.query(sql, param,
                    new SecurityRowMappers.HierarchicalResultExtractor());
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Permission Denied for retrieval for path '{}' for user '{}'",
                    name, user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_GET_FAILED, name, clazz.toString());
        }
    }


    // todo: return all resources or only leave nodes? --> currently only leaves are returned
    // todo: access to leave node also means that the path to the root for that permission is allowed,
    // todo: thus all upper resource access is as well allowed

    //todo: remove not used context?! --> who is allowed to do so?
    @Override
    public int deletePolicy (SecurityPolicy policy, User user)
            throws KustvaktException {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("id", policy.getID());

        try {
            this.jdbcTemplate.update(
                    "DELETE FROM group_ref WHERE policy_id=:id", param);
            return this.jdbcTemplate.update(
                    "DELETE FROM policy_store WHERE id=:id", param);
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Operation (DELETE) not possible for '{}' for user '{}'",
                    policy.toString(), user.getId());
            throw new dbException(user.getId(), "policy_store, group_ref",
                    StatusCodes.DB_DELETE_FAILED, policy.toString());
        }
    }


    @Override
    public int deleteResourcePolicies (String id, User user)
            throws KustvaktException {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("id", id);
        String sql = "DELETE FROM policy_store WHERE target_id in (SELECT id FROM resource_store WHERE persistent_id=:id);";
        try {
            return this.jdbcTemplate.update(sql, param);
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Operation (DELETE) not possible for '{}' for user '{}'",
                    id, user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_DELETE_FAILED, id);
        }
    }


    @Override
    public int updatePolicy (SecurityPolicy policy, User user)
            throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        np.addValue("posix", policy.getPermissionByte());
        np.addValue("en", policy.getContext().getStart());
        np.addValue("ex", policy.getContext().getEnd());
        np.addValue("id", policy.getID());

        try {
            int result = this.jdbcTemplate.update(
                    "UPDATE policy_store SET posix=:posix WHERE id=:id", np);
            this.mapConstraints(policy);
            return result;
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Operation (UPDATE) not possible for '{}' for user '{}'",
                    policy.toString(), user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_UPDATE_FAILED, policy.toString());
        }
    }


    @Override
    public int checkPolicy (SecurityPolicy policy, User user)
            throws KustvaktException {
        if (policy.getID() == -1)
            return 0;

        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("id", policy.getID());
        String sql1 = "SELECT COUNT(*) FROM policy_store AS p WHERE p.id=:id;";

        try {
            return this.jdbcTemplate.queryForObject(sql1, param, Integer.class);
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Operation (SELECT) not possible for '{}' for user '{}'",
                    policy.getTarget(), user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_GET_FAILED, policy.toString());
        }
    }


    /**
     * checks if the user is a member of the specified group.
     * Additional ownership can be tested via boolean flag
     * 
     * @param user
     * @param group
     * @param owner
     * @return
     * @throws KustvaktException
     */
    @Override
    public int matchCondition (User user, String group, boolean owner)
            throws KustvaktException {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("userid", user.getId());
        param.addValue("group", group);
        param.addValue("isadmin", BooleanUtils.getBoolean(owner));
        String sql;
        if (owner) {
            sql = "SELECT COUNT(*) FROM group_users AS gu INNER JOIN group_store AS gs "
                    + "ON gs.name=gu.group_id WHERE gu.user_id=:userid "
                    + "AND gs.name=:group AND gu.admin=:isadmin;";
        }
        else {
            sql = "SELECT COUNT(*) FROM group_users AS gu INNER JOIN group_store AS gs "
                    + "ON gs.name=gu.group_id WHERE gu.user_id=:userid "
                    + "AND gs.name=:group;";
        }

        try {
            return this.jdbcTemplate.queryForObject(sql, param, Integer.class);
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Operation (SELECT) not possible for '{}' for user '{}'",
                    group, user.getId());
            throw new dbException(user.getId(), "policy_store",
                    StatusCodes.DB_GET_FAILED, group);
        }
    }


    private void createCondition (PolicyCondition condition, User user)
            throws KustvaktException {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("name", condition.getSpecifier());
        param.addValue("ex", condition.getFlags().get(Attributes.EXPORT));
        param.addValue("qo", condition.getFlags().get(Attributes.QUERY_ONLY));
        param.addValue("com", condition.getFlags().get(Attributes.COMMERCIAL));
        param.addValue("sy", condition.getFlags().get(Attributes.SYM_USE));
        param.addValue("ex", condition.getFlags().get(Attributes.LICENCE));
        try {
            this.jdbcTemplate.update(
                    "INSERT INTO group_store (name, sym_use, export, commercial) "
                            + "VALUES (:name, :sy, :ex, :com);", param);
        }
        catch (DataAccessException e) {
            jlog.error("Operation (INSERT) not possible for '{}'",
                    condition.toString());
            throw new dbException(user.getId(), "group_store",
                    StatusCodes.DB_INSERT_FAILED, condition.toString());
        }
    }


    //todo: check for unique constraint exception and exclude from throw!
    @Override
    public int addToCondition (String username, PolicyCondition condition,
            boolean admin) throws KustvaktException {
        final String insert = "INSERT INTO group_users (user_id, group_id, admin) "
                + "VALUES ((SELECT id FROM korap_users "
                + "WHERE username=:username), :group, :status);";
        try {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue("group", condition.getSpecifier());
            param.addValue("username", username);
            param.addValue("status", BooleanUtils.getBoolean(admin));
            return this.jdbcTemplate.update(insert, param);
        }
        catch (DataAccessException e) {
            //todo: test with mysql
            if (!e.getMessage().toLowerCase().contains("UNIQUE".toLowerCase())) {
                jlog.error(
                        "Operation (INSERT) not possible for '{}' for user '{}'",
                        condition.toString(), username);
                throw new dbException(null, "group_store",
                        StatusCodes.DB_INSERT_FAILED, condition.toString());
            }
            return 0;
        }
    }


    /**
     * @param usernames
     * @param condition
     * @param admin
     * @return
     * @throws KustvaktException
     *             userID and group_id have a unique constraint,
     *             thus: if any of the supplied users is already a
     *             member of the group, the entire chain will be
     *             broken!
     */
    //todo definitely needs rework
    //todo: test the unique index constraints!
    @Override
    public int[] addToCondition (List<String> usernames,
            PolicyCondition condition, boolean admin) throws KustvaktException {
        MapSqlParameterSource[] sources = new MapSqlParameterSource[usernames
                .size()];

        //        todo: use unique index for that! problematic though --> why? no special exception?
        //        final String select = "select count(id) from group_users where userID=" +
        //                "(select id from korap_users where username=:username) " +
        //                "AND group_id=:group;";

        //todo: use index to create uniqueness. how to batch?
        final String insert = "INSERT INTO group_users (user_id, group_id, admin) "
                + "VALUES ((SELECT id FROM korap_users "
                + "WHERE username=:username), :group, :status);";
        try {
            for (int idx = 0; idx < usernames.size(); idx++) {
                //todo: dont do that here
                if (usernames.get(idx) == null || usernames.get(idx).isEmpty())
                    throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);

                MapSqlParameterSource param = new MapSqlParameterSource();
                param.addValue("group", condition.getSpecifier());
                param.addValue("username", usernames.get(idx));
                param.addValue("status", BooleanUtils.getBoolean(admin));
                // if primary keys uniqueness is determined by both keys, then use
                // that as checkup (may also be manageable via triggers)
                //                if (this.jdbcTemplate
                //                        .queryForObject(select, param, Integer.class) == 0)
                sources[idx] = param;
            }

            // todo: only insert if user is not already a member of this group
            //fixme: problem - unique constraints throws exception. skip that user entry?!
            return this.jdbcTemplate.batchUpdate(insert, sources);
        }
        catch (DataAccessException e) {
            if (!e.getCause().toString().contains("UNIQUE")) {
                jlog.error(
                        "Operation (INSERT) not possible for '{}' for user '{}'",
                        condition.toString(), usernames, e);
                throw new KustvaktException(
                        "Operation (INSERT) not possible for '"
                                + condition.toString() + "' for user '"
                                + usernames + "'", e,
                        StatusCodes.CONNECTION_ERROR);
            }
            return null;
        }
    }


    @Override
    public void removeFromCondition (List<String> usernames,
            PolicyCondition condition) throws KustvaktException {
        MapSqlParameterSource[] sources = new MapSqlParameterSource[usernames
                .size()];
        int idx = 0;
        for (String s : usernames) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue("group", condition.getSpecifier());
            param.addValue("username", s);
            sources[idx++] = param;
        }

        final String del = "DELETE FROM group_users WHERE group_id=:group AND user_id=(SELECT id FROM "
                + "korap_users WHERE username=:username);";

        try {
            this.jdbcTemplate.batchUpdate(del, sources);
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Operation (DELETE) not possible for '{}' for user '{}'",
                    condition.toString(), usernames);
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }


    @Override
    public int createParamBinding (Parameter param) throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("key", param.getName());
        source.addValue("policy", param.getPolicy().getID());
        source.addValue("value", param.getValue());
        source.addValue("flag", param.isEqual());

        //todo:
        //        if (!parameterExists(param.getName()))
        //            createParameter(param.getName(), "", param.getOwner());
        final String insert = "INSERT INTO param_map (param_id, policy_id, value, flag) VALUES ((SELECT id FROM param_store "
                + "WHERE p_key=:key), (SELECT id FROM policy_store WHERE id=:policy), :value, :flag);";
        try {
            return this.jdbcTemplate.update(insert, source);
        }
        catch (DataAccessException e) {
            jlog.error("Operation (INSERT) not possible for '{}",
                    param.toString());
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }


    @Override
    public List<String> getUsersFromCondition (PolicyCondition condition)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("specifier", condition.getSpecifier());
        final String sql1 = "SELECT username FROM korap_users WHERE id IN (SELECT user_id FROM "
                + "group_users WHERE group_id=:specifier);";
        try {
            return this.jdbcTemplate.queryForList(sql1, source, String.class);
        }
        catch (DataAccessException e) {
            jlog.error("Operation (SELECT) not possible for '{}'",
                    condition.toString());
            throw new KustvaktException(StatusCodes.CONNECTION_ERROR);
        }
    }


    private boolean parameterExists (String key) {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("key", key);
        final String select = "SELECT COUNT(*) FROM param_store WHERE p_key=:key;";
        return this.jdbcTemplate.queryForObject(select, source, Integer.class) == 1;
    }


    private void createParameter (String parameter, String value, Integer owner)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("name", parameter);
        source.addValue("value", value);
        source.addValue("owner", owner);
        final String sql = "INSERT INTO param_store (p_key, p_value) VALUES (:name, :value);";
        try {
            this.jdbcTemplate.update(sql, source);
        }
        catch (DataAccessException e) {
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }


    @Override
    public int removeParamBinding (SecurityPolicy policy)
            throws KustvaktException {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", policy.getID());
        final String sql = "DELETE FROM param_map WHERE policy_id=:id";
        try {
            return this.jdbcTemplate.update(sql, source);
        }
        catch (DataAccessException e) {
            throw new KustvaktException(e, StatusCodes.CONNECTION_ERROR);
        }
    }


    @Override
    public int size () {
        String sql = "SELECT COUNT(*) FROM policy_view;";
        try {
            return this.jdbcTemplate.queryForObject(sql,
                    new HashMap<String, Object>(), Integer.class);
        }
        catch (DataAccessException e) {
            return 0;
        }
    }

}
