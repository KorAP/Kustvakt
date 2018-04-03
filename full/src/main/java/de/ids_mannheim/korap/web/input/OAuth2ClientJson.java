package de.ids_mannheim.korap.web.input;

import de.ids_mannheim.korap.constant.ClientType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OAuth2ClientJson {
    
    // all required for registration
    private String name;
    private ClientType type;
    private String url;
    private String redirectURI;
}
