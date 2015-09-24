package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.user.Attributes;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: hanl
 * Date: 10/29/13
 * Time: 4:30 PM
 */
@Getter
public class PolicyCondition implements Comparable<PolicyCondition> {

    private static Map<String, Object> stats = new HashMap<>();

    static {
        stats.put(Attributes.SYM_USE, -1);
        stats.put(Attributes.COMMERCIAL, false);
        //fixme: doesnt query only and export infer the same thing?
        stats.put(Attributes.QUERY_ONLY, false);
        stats.put(Attributes.EXPORT, false);
        stats.put(Attributes.LICENCE, null);
        stats.put(Attributes.RANGE, null);
        //fixme: range is valuable in this context, but time span should remain in the policy context!
        stats.put(Attributes.TIME_SPANS, null);
    }

    //todo: loadSubTypes these from database or configuration --> use id reference, rather than variable declaration

    //todo: old regex for format gr(2323):  "(^[^\\(]+)\\((.*)\\)"
    //    private static final Pattern p = Pattern.compile("\\((.*)\\)");
    private final String specifier;
    private String description;
    private static final String EX_PRE = "ex:";
    private Map<String, Object> flags;

    public PolicyCondition(String target) {
        // pattern to map extensionally created groups
        this.specifier = target;
        this.flags = new HashMap<>(stats);
    }

    public PolicyCondition() {
        this(EX_PRE + createGroupName());
    }

    @Deprecated
    //todo: do this in crypto bean!
    private static String createGroupName() {
        //        return Base64.encodeBase64String(SecureRGenerator
        //                .getNextSecureRandom(64));
        return "<new group name>";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFlag(String key, Object value) {
        Object f = this.flags.get(key);
        if (f != null && f.getClass().equals(value.getClass()))
            this.flags.put(key, value);
    }

    public String getSpecifier() {
        return this.specifier;
    }

    public boolean isExtensional() {
        return getSpecifier().startsWith(EX_PRE);
    }

    @Override
    public String toString() {
        return "(" + this.specifier + ")";
    }

    @Override
    public int compareTo(PolicyCondition o) {
        return this.getSpecifier().compareTo(o.getSpecifier());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PolicyCondition that = (PolicyCondition) o;
        return specifier.equals(that.specifier);
    }

    @Override
    public int hashCode() {
        return specifier.hashCode();
    }

}
