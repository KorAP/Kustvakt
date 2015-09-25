package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 15/06/2015
 */
public abstract class AuthenticationManagerIface {

    private Map<String, AuthenticationIface> providers;

    public AuthenticationManagerIface() {
        this.providers = new HashMap<>();
    }

    public void setProviders(Set<AuthenticationIface> providers) {
        for (AuthenticationIface i : providers)
            this.providers.put(i.getIdentifier(), i);
    }

    protected AuthenticationIface getProvider(String key) {
        AuthenticationIface iface;
        if (key == null)
            iface = this.providers.get(Attributes.API_AUTHENTICATION);
        else
            iface = this.providers.get(key.toUpperCase());
        return iface;
    }

    public abstract TokenContext getTokenStatus(String token, String host,
            String useragent) throws KustvaktException;

    public abstract User getUser(String username) throws KustvaktException;

    public abstract User authenticate(int type, String username,
            String password, Map<String, Object> attributes)
            throws KustvaktException;

    public abstract TokenContext createTokenContext(User user,
            Map<String, Object> attr, String provider_key)
            throws KustvaktException;

    public abstract void logout(TokenContext context) throws KustvaktException;

    public abstract void lockAccount(User user) throws KustvaktException;

    public abstract User createUserAccount(Map<String, Object> attributes)
            throws KustvaktException;

    public abstract boolean updateAccount(User user) throws KustvaktException;

    public abstract boolean deleteAccount(User user) throws KustvaktException;

    public abstract UserDetails getUserDetails(User user) throws
            KustvaktException;

    public abstract UserSettings getUserSettings(User user)
            throws KustvaktException;

    public abstract void updateUserDetails(User user, UserDetails details)
            throws KustvaktException;

    public abstract void updateUserSettings(User user, UserSettings settings)
            throws KustvaktException;

    public abstract Object[] validateResetPasswordRequest(String username,
            String email) throws KustvaktException;

    public abstract void resetPassword(String uriFragment, String username,
            String newPassphrase) throws KustvaktException;

    public abstract void confirmRegistration(String uriFragment,
            String username) throws KustvaktException;

    @Override
    public String toString() {
        return "provider list: " + this.providers.toString();
    }
}
