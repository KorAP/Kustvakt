package de.ids_mannheim.korap.constant;

public enum OAuth2ClientType {

    // EM: from RFC 6749
        
//    Clients capable of maintaining the confidentiality of their
//    credentials (e.g., client implemented on a secure server with
//    restricted access to the client credentials), or capable of secure
//    client authentication using other means.
    CONFIDENTIAL, 
    
//    Clients incapable of maintaining the confidentiality of their
//    credentials (e.g., clients executing on the device used by the
//    resource owner, such as an installed native application or a web
//    browser-based application), and incapable of secure client
//    authentication via any other means. Mobile and Javascript apps 
//    are considered public clients. 
    PUBLIC;
}
