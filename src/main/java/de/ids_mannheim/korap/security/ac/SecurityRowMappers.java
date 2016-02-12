package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.ResourceFactory;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.PolicyContext;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.utils.PrefixTreeMap;
import lombok.Data;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author hanl
 * @date 03/03/2014
 */
public class SecurityRowMappers {

    public static class PolicyRowMapper implements RowMapper<SecurityPolicy> {

        @Override
        public SecurityPolicy mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            SecurityPolicy p = new SecurityPolicy();
            p.setID(rs.getInt("pid")).setTarget(rs.getString("persistent_id"))
                    .setPOSIX(rs.getString("perm"));

            PolicyContext context = new PolicyContext();
            context.setIPMask(rs.getString("iprange"));
            long enable = rs.getLong("enable");
            long expire = rs.getLong("expire");
            if (enable != -1)
                context.setEnableTime(enable);
            if (expire != -1)
                context.setExpirationTime(expire);
            //            context.addFlag("export", rs.getBoolean("export"));
            //            context.addFlag("sym_use", rs.getInt("sym_use"));
            p.setContext(context);
            return p;
        }
    }

    @Data
    public static class FlagContext extends PolicyContext {

        private Map<String, Object> flags;

        public FlagContext() {
            this.flags = new HashMap<>();
        }

        public FlagContext addFlag(String key, Object value) {
            this.flags.put(key, value);
            return this;
        }

        public FlagContext removeFlag(String key) {
            this.flags.remove(key);
            return this;
        }

        public FlagContext clearFlags() {
            this.flags.clear();
            return this;
        }
    }

    public static List<SecurityPolicy>[] mapping(ResultSet rs)
            throws SQLException {
        List<SecurityPolicy>[] policyArray = null;
        List<Integer>[] idx = null;
        while (rs.next()) {
            // user has no permission here --> thus skip
            if (rs.getInt("allowed") == 0)
                continue;

            if (policyArray == null) {
                int v = rs.getInt("max_depth") + 1;
                policyArray = new List[v];
                idx = new List[v];
            }

            int depth = rs.getInt("depth");

            if (policyArray[depth] == null) {
                policyArray[depth] = new ArrayList<>();
                idx[depth] = new ArrayList<>();
            }

            Integer pid = rs.getInt("pid");
            String grouping = rs.getString("group_id");
            Integer index = idx[depth].indexOf(pid);

            SecurityPolicy policy;
            if (index == -1) {
                if (pid == -1 && grouping.equalsIgnoreCase("self")) {
                    policy = new SecurityPolicy.OwnerPolicy(
                            rs.getString("persistent_id"),
                            rs.getInt("creator"));
                    policyArray[depth].add(0, policy);
                    idx[depth].add(0, pid);
                }else {
                    policy = new SecurityRowMappers.PolicyRowMapper()
                            .mapRow(rs, 0);
                    policyArray[depth].add(policy);
                    idx[depth].add(pid);

                    //todo:
                    //                    if (policy.isActive(user)) {
                    //                        policyArray[depth].add(policy);
                    //                        idx[depth].add(pid);
                    //                    }
                }
            }else
                policy = policyArray[depth].get(index);

            PolicyCondition c = new PolicyCondition(rs.getString("group_id"));
            if (!policy.contains(c))
                policy.addCondition(c);
        }
        return policyArray;
    }

    @Deprecated
    public static List<SecurityPolicy>[] map(ResultSet rs) throws SQLException {
        Map<Integer, SecurityPolicy>[] policyArray = null;
        while (rs.next()) {
            // user has no permission here!
            if (rs.getInt("allowed") == 0)
                continue;

            if (policyArray == null)
                policyArray = new Map[rs.getInt("max_depth") + 1];

            int depth = rs.getInt("depth");
            Map<Integer, SecurityPolicy> cursor = policyArray[depth];
            if (cursor == null)
                cursor = new HashMap<>();

            Integer pid = rs.getInt("pid");
            SecurityPolicy policy = cursor.get(pid);
            if (policy == null) {
                policy = new SecurityRowMappers.PolicyRowMapper().mapRow(rs, 0);
                cursor.put(pid, policy);
            }
            PolicyCondition c = new PolicyCondition(rs.getString("group_ref"));

            if (!policy.contains(c))
                policy.addCondition(c);
        }

        List<SecurityPolicy>[] results;
        if (policyArray == null) {
            results = new List[1];
            results[0] = new ArrayList<>();
        }else {
            results = new List[policyArray.length];
            for (int idx = 0; idx < policyArray.length; idx++) {
                if (policyArray[idx] != null)
                    results[idx] = new ArrayList<>(policyArray[idx].values());
                else
                    results[idx] = new ArrayList<>();
            }
        }
        return results;
    }

    public static class HierarchicalResultExtractor
            implements ResultSetExtractor<List<KustvaktResource.Container>> {

        private boolean _withpid;

        //        public HierarchicalResultExtractor(boolean wpid) {
        //            this._withpid = wpid;
        //        }

        // todo: in order for this to work, all parent flags need to be matched in sql!
        public List<KustvaktResource.Container> extractData(ResultSet rs)
                throws SQLException, DataAccessException {
            // contains the container with the highest available name_path to retrieve partial matches!
            PrefixTreeMap<KustvaktResource.Container[]> containerMap = new PrefixTreeMap<>();
            Map<Integer, SecurityPolicy> trace = new HashMap<>();

            while (rs.next()) {
                KustvaktResource.Container[] cursor;
                Integer pid = rs.getInt("pid");

                SecurityPolicy policy = trace.get(pid);
                if (policy == null | pid == -1) {
                    //                    Integer id = rs.getInt("id");
                    String persistentId = rs.getString("persistent_id");
                    int depth = rs.getInt("depth");
                    String namePath = rs.getString("name_path");
                    policy = new SecurityRowMappers.PolicyRowMapper()
                            .mapRow(rs, 0);

                    //todo: put active status here!
                    trace.put(pid, policy);

                    //fixme: since leaves are mentioned first, maybe retrieve
                    SortedMap<String, KustvaktResource.Container[]> submatch;
                    if ((submatch = containerMap.getPrefixSubMap(namePath))
                            == null) {
                        //create container for last child node
                        cursor = new KustvaktResource.Container[depth + 1];
                        cursor[depth] = new KustvaktResource.Container(
                                persistentId,
                                ResourceFactory.getResource(rs.getInt("type"))
                                        .getClass());
                        containerMap.put(namePath, cursor);
                    }else {
                        KustvaktResource.Container[] values = submatch
                                .get(submatch.firstKey());
                        values[depth] = new KustvaktResource.Container(
                                persistentId,
                                ResourceFactory.getResource(rs.getInt("type"))
                                        .getClass());
                    }
                }
            }

            List<KustvaktResource.Container> result = new ArrayList<>();
            for (KustvaktResource.Container[] values : containerMap.values()) {
                for (KustvaktResource.Container container : values)
                    if (container == null)
                        containerMap.remove(values);
                result.add(values[values.length - 1]);
            }
            return result;
        }
    }

}
