package de.ids_mannheim.korap.security.ac;

import de.ids_mannheim.korap.exceptions.NotAuthorizedException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.security.PermissionsBuffer;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hanl on 5/22/14.
 */
public class PolicyEvaluator {

    private static final Logger jlog = LoggerFactory
            .getLogger(PolicyEvaluator.class);

    private final User user;
    private final List<SecurityPolicy>[] policies;
    private String resourceID;
    private PermissionsBuffer permissions;
    private boolean processed;
    private int relationError = -1;
    @Deprecated
    private Map<String, Object> flags;

    public PolicyEvaluator(User user, List<SecurityPolicy>[] policies) {
        this.user = user;
        this.policies = policies;
        this.permissions = new PermissionsBuffer();
        this.flags = new HashMap<>();
    }

    private PolicyEvaluator(User user, KustvaktResource resource) {
        this.user = user;
        this.resourceID = resource.getPersistentID();
        this.permissions = new PermissionsBuffer();
        this.flags = new HashMap<>();
        this.policies = null;
    }

    public String getResourceID() {
        if (this.resourceID == null && policies[0] != null
                && policies[0].get(0) != null)
            this.resourceID = policies[0].get(0).getTarget();
        return this.resourceID;
    }

    // todo: test benchmarks
    private List<SecurityPolicy> evaluate(List<SecurityPolicy>[] policies,
            Permissions.Permission perm) throws NotAuthorizedException {
        //fixme: what happens in case a parent relation does not allow changing a resource, but the owner of child per default
        //todo: receives all rights? --> test casing
        jlog.error("IS USER RESOURCE OWNER? " + isOwner());
        if (isOwner()) {
            jlog.debug("Resource is owned by the user!");
            return policies[0];
        }
        if (!processed && policies != null) {
            for (int i = policies.length - 1; i >= 0; i--) {
                int idx = 0;
                if (policies[i] != null) {
                    int ow = getOwner(policies[i]);
                    for (int internal = 0;
                         internal < policies[i].size(); internal++) {
                        SecurityPolicy s = policies[i].get(internal);
                        if (i == policies.length - 1) {
                            if (ow == user.getId())
                                this.permissions.addPermission(127);
                            else if (!(s instanceof SecurityPolicy.OwnerPolicy))
                                this.permissions
                                        .addPermission(s.getPermissionByte());
                        }else {
                            if (ow == user.getId())
                                this.permissions.retain(127);
                            else if (!(s instanceof SecurityPolicy.OwnerPolicy))
                                this.permissions.retain(s.getPermissionByte());
                        }
                        idx++;
                    }
                }
                // fixme: what is that?
                if (idx == 0) {
                    relationError = i;
                    throw new NotAuthorizedException(
                            StatusCodes.PERMISSION_DENIED,
                            this.getResourceID());
                }
            }
            this.processed = true;
            System.out.println("FINAL BYTE :" + this.permissions.getPbyte());
            if (this.permissions.containsPermission(perm))
                return policies[0];
        }else if (processed && relationError == -1 && this.permissions
                .containsPermission(perm)) {
            jlog.debug("Done processing resource policies");
            jlog.debug("Will return policies to security manager: "
                    + this.policies[0]);
            return this.policies[0];
        }

        return Collections.emptyList();
    }

    /**
     * checks read permission
     *
     * @return
     */
    public boolean isAllowed() {
        return isAllowed(Permissions.Permission.READ);
    }

    public boolean isAllowed(Permissions.Permission perm) {
        try {
            List s = evaluate(this.policies, perm);
            return s != null && !s.isEmpty();
        }catch (NotAuthorizedException e) {
            return false;
        }
    }

    public boolean isOwner() {
        return policies != null && this.user.getId() != null
                && getOwner(this.policies[0]) == this.user.getId();
    }

    private int getOwner(List<SecurityPolicy> policies) {
        if (policies != null && policies.get(0) != null && policies
                .get(0) instanceof SecurityPolicy.OwnerPolicy) {
            return ((SecurityPolicy.OwnerPolicy) policies.get(0)).getOwner();
        }
        return -1;
    }

    // todo: what is this supposed to do?
    @Deprecated
    public static PolicyEvaluator setFlags(User user,
            KustvaktResource resource) {
        PolicyEvaluator e = new PolicyEvaluator(user, resource);
//        e.setFlag("managed", resource.getOwner() == KorAPUser.ADMINISTRATOR_ID);
//        e.setFlag("shared", false);
        return e;
    }

    public <V> V getFlag(String key, V value) {
        return (V) this.flags.get(key);
    }

    private <V> void setFlag(String key, V value) {
        this.flags.put(key, value);
    }

    public boolean isManaged() {
        return getOwner(this.policies[0]) == KorAPUser.ADMINISTRATOR_ID;
    }

    public boolean isShared() {
        return !isManaged() && !isOwner();
    }

}
