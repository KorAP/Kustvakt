package de.ids_mannheim.korap.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;

import de.ids_mannheim.korap.config.TokenType;
import de.ids_mannheim.korap.config.AuthenticationMethod;
import de.ids_mannheim.korap.config.AuthenticationScheme;
import de.ids_mannheim.korap.config.KustvaktCacheable;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.Userdata;

/**
 * @author hanl
 * @date 15/06/2015
 */
public abstract class AuthenticationManagerIface extends KustvaktCacheable {

    private Map<TokenType, AuthenticationIface> providers;


    public AuthenticationManagerIface () {
        super("id_tokens", "key:id_tokens");
        this.providers = new HashMap<>();
    }


    public void setProviders (Set<AuthenticationIface> providers) {
        for (AuthenticationIface i : providers){
            this.providers.put(i.getTokenType(), i);
        }
    }


    protected AuthenticationIface getProvider (TokenType scheme, TokenType default_iface) {
    	
    	// Debug FB: loop a Map
    	
    	 /*for (Map.Entry<String, AuthenticationIface> entry : this.providers.entrySet()) 
    		{
    		System.out.println("Debug: provider: Key : " + entry.getKey() + " Value : " + entry.getValue());
    		}
    		*/
     // todo: configurable authentication schema
        if (scheme == null){ 
            return this.providers.get(default_iface);
        }
        else{
            return this.providers.get(scheme);
        }
    }


    public abstract TokenContext getTokenContext (TokenType type,
            String token, String host, String useragent)
            throws KustvaktException;


    public abstract User getUser (String username) throws KustvaktException;

    public abstract boolean isRegistered (String id);


    public abstract User authenticate (AuthenticationMethod method, String username,
            String password, Map<String, Object> attributes)
            throws KustvaktException;

    //    public abstract User authenticate (int type, String username,
    //            String password, Map<String, Object> attributes)
    //            throws KustvaktException;


    public abstract TokenContext createTokenContext (User user,
            Map<String, Object> attr, TokenType type)
            throws KustvaktException;
    
//    public abstract TokenContext createTokenContext (User user,
//            Map<String, Object> attr, String provider_key)
//            throws KustvaktException;

    public abstract void setAccessAndLocation (User user, HttpHeaders headers);

    public abstract void logout (TokenContext context) throws KustvaktException;


    public abstract void lockAccount (User user) throws KustvaktException;


    public abstract User createUserAccount (Map<String, Object> attributes,
            boolean confirmation_required) throws KustvaktException;


    //    public abstract boolean updateAccount(User user) throws KustvaktException;

    public abstract boolean deleteAccount (User user) throws KustvaktException;


    public abstract <T extends Userdata> T getUserData (User user,
            Class<T> clazz) throws KustvaktException;


    public abstract void updateUserData (Userdata data)
            throws KustvaktException;


    public abstract Object[] validateResetPasswordRequest (String username,
            String email) throws KustvaktException;


    public abstract void resetPassword (String uriFragment, String username,
            String newPassphrase) throws KustvaktException;


    public abstract void confirmRegistration (String uriFragment,
            String username) throws KustvaktException;


    public String providerList () {
        return "provider list: " + this.providers.toString();
    }


    public abstract User getUser (String username, String method)
            throws KustvaktException;

}
