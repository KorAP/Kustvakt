package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.UserDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hanl
 * @date 09/12/2014
 */
public class Scopes {

    public enum Scope {
        openid, profile, email, queries, account, preferences, search

    }

    private static final String[] profile = { Attributes.EMAIL,
            Attributes.FIRSTNAME, Attributes.LASTNAME, Attributes.INSTITUTION,
            Attributes.ADDRESS, Attributes.PHONE, Attributes.GENDER,
            Attributes.COUNTRY };

    private static final String[] OPENID_CONNECT = { Scope.profile.toString() };

    private static final Enum[] SERVICE_DEFAULTS = { Scope.account,
            Scope.preferences, Scope.search, Scope.queries };

    public static Map<String, Object> getProfileScopes(
            Map<String, Object> values) {
        Map<String, Object> r = new HashMap<>();
        for (String key : profile) {
            Object v = values.get(key);
            if (v != null)
                r.put(key, v);
        }
        return r;
    }

    /**
     * expects space separated values
     *
     * @param scopes
     * @return
     */
    //todo: test
    public static Enum[] mapScopes(String scopes) {
        List<Enum> s = new ArrayList<>();
        for (String value : scopes.split(" "))
            s.add(Scope.valueOf(value.toLowerCase()));
        return (Enum[]) s.toArray(new Enum[s.size()]);
    }

    public static Map<String, Object> mapOpenIDConnectScopes(String scopes,
            UserDetails details) {
        Map<String, Object> m = new HashMap<>();
        if (scopes != null && !scopes.isEmpty()) {
            scopes = scopes.toLowerCase();
            if (scopes.contains(Scope.email.toString()))
                m.put(Attributes.EMAIL, details.getEmail());
            if (scopes.contains(Scope.profile.toString()))
                m.putAll(Scopes.getProfileScopes(details.toMap()));
        }
        return m;
    }



}
