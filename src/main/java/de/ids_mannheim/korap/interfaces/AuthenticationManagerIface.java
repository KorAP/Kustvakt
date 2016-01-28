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
            this.providers.put(i.getIdentifier().toUpperCase(), i);
    }

    protected AuthenticationIface getProvider(String key,
            String default_iface) {
        AuthenticationIface iface = this.providers
                .get(key != null ? key.toUpperCase() : "NONE");
        // todo: configurable authentication schema
        if (iface == null)
            iface = this.providers.get(default_iface);
        return iface;
    }

    public abstract TokenContext getTokenStatus(String token, String host,
            String useragent) throws KustvaktException;

    public abstract User getUser(String username) throws KustvaktException;

    public abstract User authenticate(int type, String username,
            String password, Map<String, String> attributes)
            throws KustvaktException;

    public abstract TokenContext createTokenContext(User user,
            Map<String, String> attr, String provider_key)
            throws KustvaktException;

    public abstract void logout(TokenContext context) throws KustvaktException;

    public abstract void lockAccount(User user) throws KustvaktException;

    public abstract User createUserAccount(Map attributes,
            boolean confirmation_required) throws KustvaktException;

    //    public abstract boolean updateAccount(User user) throws KustvaktException;

    public abstract boolean deleteAccount(User user) throws KustvaktException;

    public abstract <T extends Userdata> T getUserData(User user,
            Class<T> clazz) throws KustvaktException;

    public abstract void updateUserData(Userdata data) throws KustvaktException;

    @Deprecated
    public abstract UserDetails getUserDetails(User user)
            throws KustvaktException;

    @Deprecated
    public abstract UserSettings getUserSettings(User user)
            throws KustvaktException;

    @Deprecated
    public abstract void updateUserDetails(User user, UserDetails details)
            throws KustvaktException;

    @Deprecated
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
