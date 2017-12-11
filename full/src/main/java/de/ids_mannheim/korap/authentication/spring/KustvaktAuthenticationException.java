package de.ids_mannheim.korap.authentication.spring;

import org.springframework.security.core.AuthenticationException;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.CoreResponseHandler;

public class KustvaktAuthenticationException extends AuthenticationException {

    /**
     * Auto-generated serian UID
     */
    private static final long serialVersionUID = -357703101436703635L;
    private String notification;
    
    public KustvaktAuthenticationException (String msg) {
        super(msg);
    }

    public KustvaktAuthenticationException (KustvaktException e) {
        super(e.getMessage());
        notification = CoreResponseHandler.buildNotification(e.getStatusCode(),
                e.getMessage(), e.getEntity());
    }
    
    public String getNotification () {
        return notification;
    }
}
