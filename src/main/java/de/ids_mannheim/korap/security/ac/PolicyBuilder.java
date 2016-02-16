package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.resources.Relation;
import de.ids_mannheim.korap.response.Notifications;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.PolicyContext;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 14/04/2014
 */

// todo: also be able to create or edit relations
public class PolicyBuilder {

    private Notifications notifications;
    private User user;
    private KustvaktResource[] resources;
    private KustvaktResource[] parents;
    private Permissions.Permission[] permissions;
    private PolicyCondition[] conditions;
    //    private Map<String, ParameterSettingsHandler> settings;
    private Relation rel = null;
    private PolicyContext context;

    public PolicyBuilder(User user) {
        this.user = user;
        this.notifications = new Notifications();
        // fixme: other exception!?
        if (this.user.getId() == -1)
            throw new RuntimeException("user id must be a valid interger id");
    }

    public PolicyBuilder setResources(KustvaktResource... targets) {
        this.resources = targets;
        this.parents = new KustvaktResource[targets.length];
        return this;
    }

    /**
     * set the parents for the resources. Order is relevant, since the relation parent - resource is handled
     * via the index within the array. Parent relation is limited to depth 1!
     * In case of a skipped parent resource relation within the array, set 'null'
     *
     * @param parents
     * @return
     */
    public PolicyBuilder setParents(KustvaktResource... parents) {
        for (int idx = 0; idx < parents.length; idx++)
            this.parents[idx] = parents[idx];
        return this;
    }

    public PolicyBuilder setContext(PolicyContext context) {
        this.context = context;
        return this;
    }

    public PolicyBuilder setContext(long start, long end) {
        if (this.context == null)
            this.context = new PolicyContext();
        this.context.setEnableTime(start);
        this.context.setExpirationTime(end);
        return this;
    }

    public PolicyBuilder setLocation(String iprange) {
        if (this.context == null)
            this.context = new PolicyContext();
        this.context.setIPMask(iprange);
        return this;
    }

    public PolicyBuilder setPermissions(
            Permissions.Permission... permissions) {
        this.permissions = permissions;
        return this;
    }

    public PolicyBuilder setConditions(String... conditions) {
        this.conditions = new PolicyCondition[conditions.length];
        for (int idx = 0; idx < conditions.length; idx++)
            this.conditions[idx] = new PolicyCondition(conditions[idx]);
        return this;
    }

    public PolicyBuilder setConditions(PolicyCondition... conditions) {
        this.conditions = new PolicyCondition[conditions.length];
        for (int idx = 0; idx < conditions.length; idx++)
            this.conditions[idx] = conditions[idx];
        return this;
    }

    public PolicyBuilder setRelation(Relation rel) {
        this.rel = rel;
        return this;
    }

    public PolicyBuilder addCondition(String condition) {
        if (this.rel == null)
            setRelation(Relation.AND);
        return setConditions(condition);
    }

    public String create() throws KustvaktException {
        return this.doIt();
    }

    // for and relations there is no way of setting parameters conjoined with the policy
    private String doIt() throws KustvaktException {
        if (this.resources == null)
            throw new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "resource must be set",
                    "resource");
        if (this.permissions == null)
            throw new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "permissions must be set",
                    "permission");
        if (this.conditions == null)
            throw new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "conditions must be set",
                    "condition");
        if (this.rel == null)
            this.rel = Relation.AND;

        for (int idx = 0; idx < this.resources.length; idx++) {
            try {
                if (parents[idx] != null)
                    resources[idx].setParentID(parents[idx].getPersistentID());
                SecurityManager manager = SecurityManager
                        .register(resources[idx], user);

                if (rel.equals(Relation.AND)) {
                    SecurityPolicy policy = new SecurityPolicy()
                            .setConditions(this.conditions)
                            .setTarget(resources[idx])
                            .addPermission(permissions)
                            .setCreator(this.user.getId());

                    if (this.context != null)
                        policy.setContext(this.context);

                    manager.addPolicy(policy);

                }else if (rel.equals(Relation.OR)) {
                    for (PolicyCondition c : this.conditions) {
                        SecurityPolicy policy = new SecurityPolicy()
                                .addCondition(c).setTarget(resources[idx])
                                .addPermission(permissions)
                                .setCreator(this.user.getId());

                        if (this.context != null)
                            policy.setContext(this.context);

                        //todo: ???
                        //                    if (this.settings != null) {
                        //                        ParameterSettingsHandler settings = this.settings
                        //                                .get(c.getSpecifier());
                        //                        if (settings != null) {
                        //                            // fixme: context setting overlap!
                        //                            policy.setContext(settings.getContext());
                        //                            manager.addPolicy(policy, settings.getParameters());
                        //                            continue;
                        //                        }
                        //                    }
                        manager.addPolicy(policy);
                    }
                }
            }catch (KustvaktException e) {
                this.notifications.addError(e.getStatusCode(), e.getMessage(),
                        resources[idx].getPersistentID());
            }
        }
        return notifications.toJsonString();
    }
}
