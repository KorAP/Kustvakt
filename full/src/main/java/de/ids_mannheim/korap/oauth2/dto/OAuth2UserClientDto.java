package de.ids_mannheim.korap.oauth2.dto;

/** Lists authorized OAuth2 clients of a user
 * 
 * @author margaretha
 *
 */
public class OAuth2UserClientDto {
    
    private String clientId;
    private String clientName;
    
    public String getClientName () {
        return clientName;
    }
    public void setClientName (String clientName) {
        this.clientName = clientName;
    }
    public String getClientId () {
        return clientId;
    }
    public void setClientId (String clientId) {
        this.clientId = clientId;
    }
}
