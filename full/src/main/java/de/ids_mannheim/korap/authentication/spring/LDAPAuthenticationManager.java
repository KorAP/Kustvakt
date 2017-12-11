package de.ids_mannheim.korap.authentication.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.unboundid.ldap.sdk.LDAPException;

import de.ids_mannheim.korap.authentication.LdapAuth3;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

public class LDAPAuthenticationManager implements AuthenticationManager {

    private static Logger jlog =
            LoggerFactory.getLogger(LDAPAuthenticationManager.class);
    @Autowired
    private FullConfiguration config;

    @Override
    public Authentication authenticate (Authentication authentication)
            throws AuthenticationException {

        String username = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        // just to make sure that the plain password does not appear anywhere in
        // the logs!

        System.out.printf("Debug: authenticateIdM: entering for '%s'...\n",
                username);

        /**
         * wozu Apache Validatoren für User/Passwort für IdM/LDAP? siehe
         * validation.properties. Abgeschaltet 21.04.17/FB try {
         * validator.validateEntry(username, Attributes.USERNAME); } catch
         * (KustvaktException e) { throw new WrappedException(e,
         * StatusCodes.LOGIN_FAILED, username); }
         */
        if (username == null || username.isEmpty() || password == null
                || password.isEmpty()) {
            throw new KustvaktAuthenticationException(
                    new KustvaktException(StatusCodes.BAD_CREDENTIALS,
                            "Missing username or password."));
        }

        // LDAP Access:
        try {
            // todo: unknown = ...
            int ret =
                    LdapAuth3.login(username, password, config.getLdapConfig());
            System.out.printf(
                    "Debug: autenticationIdM: Ldap.login(%s) returns: %d.\n",
                    username, ret);
            if (ret != LdapAuth3.LDAP_AUTH_ROK) {
                jlog.error("LdapAuth3.login(username='{}') returns '{}'='{}'!",
                        username, ret, LdapAuth3.getErrMessage(ret));

                // mask exception to disable user guessing in possible attacks
                /*
                 * by Hanl throw new WrappedException(new
                 * KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
                 * StatusCodes.LOGIN_FAILED, username);
                 */
                throw new KustvaktAuthenticationException(new KustvaktException(
                        username, StatusCodes.LDAP_BASE_ERRCODE + ret,
                        LdapAuth3.getErrMessage(ret), username));
            }
        }
        catch (LDAPException e) {

            jlog.error("Error: username='{}' -> '{}'!", username, e);
            // mask exception to disable user guessing in possible attacks
            /*
             * by Hanl: throw new WrappedException(new
             * KustvaktException(username, StatusCodes.BAD_CREDENTIALS),
             * StatusCodes.LOGIN_FAILED, username);
             */
            throw new KustvaktAuthenticationException(new KustvaktException(
                    username,
                    StatusCodes.LDAP_BASE_ERRCODE + LdapAuth3.LDAP_AUTH_RINTERR,
                    LdapAuth3.getErrMessage(LdapAuth3.LDAP_AUTH_RINTERR),
                    username));
        }

        jlog.debug("Authentication done: " + username);
        return authentication;
    }

}
