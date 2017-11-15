package de.ids_mannheim.korap.authentication.framework;

import de.ids_mannheim.korap.config.AuthenticationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizationData {

    private String token;
    private AuthenticationType authenticationType;

}
