package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.user.User;
import lombok.Getter;

/**
 * @author hanl@ids-mannheim.de
 * @date 09/11/13
 */
//todo:
@Getter
public class Parameter extends KustvaktResource {

    private String value;
    private SecurityPolicy policy;
    // todo: what is this supposed to do?
    private boolean equal;

    public Parameter(String identifier, String value, boolean equal,
            User user) {
        super();
        super.setName(identifier.toLowerCase());
        this.value = value;
        this.equal = equal;
    }

    @Override
    public void merge(KustvaktResource resource) {
    }

    @Override
    public void checkNull() {
    }

    public String getValue() {
        if (policy == null)
            return null;
        return value;
    }

    public void setPolicy(SecurityPolicy policy) {
        this.policy = policy;
    }

}
