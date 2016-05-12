package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.Permissions;
import de.ids_mannheim.korap.user.User;
import lombok.Getter;

import java.util.*;

/**
 * User: hanl
 * Date: 10/8/13
 * Time: 5:26 PM
 */

// default is deny, but deny policies are allowed, when specifying a subset that would otherwise be allowed!
// must be implemented as a resolution mechanism, that automatically creates this kind of policy strategy (allow > deny)

public class SecurityPolicy {

    private int id = 0;
    // a settingattribute id for instance,
    // which specifies the attribute to be protected by this policy
    private String target;
    // todo: change to set!
    private List<PolicyCondition> conditions;
    private Set<Integer> removedidx;
    private Set<Integer> addedidx;
    private PermissionsBuffer permissions;
    private PolicyContext ctx;
    private Integer creator;

    public SecurityPolicy() {
        this.setID(-1);
        this.ctx = new PolicyContext();
        this.conditions = new ArrayList<>();
        this.removedidx = new HashSet<>();
        this.addedidx = new HashSet<>();
        this.permissions = new PermissionsBuffer();
    }

    public SecurityPolicy(Integer id) {
        this();
        this.setID(id);
    }

    public SecurityPolicy setID(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getID() {
        return this.id;
    }

    public Integer getCreator() {
        return this.creator;
    }

    public PolicyContext getContext() {
        return this.ctx;
    }

    public SecurityPolicy setTarget(KustvaktResource resource) {
        this.target = resource.getPersistentID();
        return this;
    }

    public SecurityPolicy setTarget(String target) {
        this.target = target;
        return this;
    }

    public String getTarget() {
        return this.target;
    }

    public SecurityPolicy setPOSIX(String posix) {
        this.permissions = new PermissionsBuffer(Short.valueOf(posix));
        return this;
    }

    public SecurityPolicy setCreator(Integer creator) {
        this.creator = creator;
        return this;
    }

    // todo ???????
    @Deprecated
    private SecurityPolicy setOverride(Permissions.Permission... perms) {
        for (Permissions.Permission p : perms)
            this.permissions.addOverride(p.toByte());
        return this;
    }

    public SecurityPolicy setContext(PolicyContext ctx) {
        this.ctx = ctx;
        return this;
    }

    private boolean hasContext() {
        return !ctx.noMask();
    }

    //todo:
    public boolean isActive(User user) {
        System.out.println("THE POLICY " + this.toString());
        System.out.println("DOES THIS HAVE CONTEXT? " + this.hasContext());
        //        String host = (String) user.getField(Attributes.HOST);
        //        System.out.println("HOST IS " + host);
        //        System.out.println("is active? " + ctx.isActive(host));
        //        if (this.hasContext())
        //            return ctx.isActive(host);
        return !this.hasContext();
    }

    public List<String> getConditionList() {
        List<String> c = new LinkedList<>();
        Collections.sort(conditions);
        for (PolicyCondition p : conditions)
            c.add(p.getSpecifier());
        return c;
    }

    public String getConditionString() {
        if (conditions.isEmpty())
            return "";

        Collections.sort(conditions);
        StringBuffer b = new StringBuffer();
        for (PolicyCondition c : conditions) {
            b.append(c);
            b.append(";");
        }
        b.deleteCharAt(b.lastIndexOf(";"));
        return b.toString();
    }

    public List<PolicyCondition> getConditions() {
        return this.conditions;
    }

    public SecurityPolicy setConditions(PolicyCondition... constraints) {
        this.conditions.clear();
        this.removedidx.clear();
        this.addedidx.clear();
        for (int idx = 0; idx < constraints.length; idx++) {
            this.conditions.add(idx, constraints[idx]);
            this.addedidx.add(idx);
        }
        return this;
    }

    public SecurityPolicy removeCondition(PolicyCondition constraint) {
        int idx = this.conditions.indexOf(constraint);
        if (this.addedidx.contains(idx))
            this.addedidx.remove(idx);
        else
            this.removedidx.add(idx);
        return this;
    }

    public SecurityPolicy addCondition(PolicyCondition constraint) {
        this.conditions.add(constraint);
        return this;
    }

    public SecurityPolicy addNewCondition(PolicyCondition constraint) {
        if (this.conditions.add(constraint))
            this.addedidx.add(this.conditions.indexOf(constraint));
        return this;
    }

    public boolean contains(PolicyCondition constraint) {
        return conditions.contains(constraint);
    }

    public Collection<Integer> getRemoved() {
        return this.removedidx;
    }

    public Collection<Integer> getAdded() {
        return this.addedidx;
    }

    public void clear() {
        // clear remove, add, conditions list!
        for (Integer remove : this.removedidx)
            this.conditions.remove(remove);
        this.removedidx.clear();
        this.addedidx.clear();
    }

    public boolean hasPermission(Permissions.Permission perm) {
        return permissions != null && permissions.containsPermission(perm);
    }

    /**
     * function to add a permission byte to the collection.
     *
     * @param perms
     * @return
     */
    public SecurityPolicy addPermission(Permissions.Permission... perms) {
        permissions.addPermissions(perms);
        return this;
    }

    public boolean equalsPermission(Permissions.Permission... perms) {
        PermissionsBuffer b = new PermissionsBuffer();
        b.addPermissions(perms);
        return permissions != null && permissions.getPbyte()
                .equals(b.getPbyte());
    }

    public void removePermission(Permissions.Permission perm) {
        if (permissions != null)
            permissions.removePermission(perm);
    }

    public Byte getPermissionByte() {
        return permissions.getPbyte();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SecurityPolicy{");
        sb.append("id=").append(id);
        sb.append(", target='").append(target).append('\'');
        sb.append(", conditions=").append(conditions);
        sb.append(", permissions=").append(getPermissions());
        sb.append('}');
        return sb.toString();
    }

    public Set<Permissions.Permission> getPermissions() {
        return permissions.getPermissions();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SecurityPolicy policy = (SecurityPolicy) o;

        if (id != policy.id)
            return false;
        if (target != policy.target)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + target.hashCode();
        return result;
    }

    @Getter
    public static class OwnerPolicy extends SecurityPolicy {
        private final Integer owner;

        public OwnerPolicy(String target, Integer owner) {
            this.owner = owner;
            super.setTarget(target);
        }

        @Override
        public String toString() {
            return "OwnerPolicy(" + super.getTarget() + "," + owner + ")";
        }

    }
}
