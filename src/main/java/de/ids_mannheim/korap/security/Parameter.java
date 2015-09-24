package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.user.User;
import lombok.Getter;

/**
 * @author hanl@ids-mannheim.de
 * @date 09/11/13
 */
@Getter
public class Parameter extends KustvaktResource {

    private String value;
    private SecurityPolicy policy;
    private boolean equality;

    public Parameter(String identifier, String value, boolean equality,
            User user) {
        super();
        super.setName(identifier.toLowerCase());
        this.value = value;
        this.equality = equality;
        super.setOwner(user.getId());
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
