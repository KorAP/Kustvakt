package de.ids_mannheim.korap.security.auth;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * implementation of the AuthenticationIface to handle korap authentication
 * internals
 *
 * @author hanl
 */
public class SessionAuthentication implements AuthenticationIface {

    private static Logger jlog = KustvaktLogger
            .getLogger(SessionAuthentication.class);
    private SessionFactory sessions;
    private ScheduledThreadPoolExecutor scheduled;
    private EncryptionIface crypto;
    private KustvaktConfiguration config;

    public SessionAuthentication(KustvaktConfiguration config,
            EncryptionIface crypto) {
        jlog.info("initialize session authentication handler");
        this.crypto = crypto;
        this.config = config;
        this.scheduled = new ScheduledThreadPoolExecutor(1);
        this.sessions = new SessionFactory(this.config.isAllowMultiLogIn(),
                this.config.getInactiveTime());
        this.scheduled.scheduleAtFixedRate(this.sessions,
                this.config.getInactiveTime() / 2,
                this.config.getInactiveTime(), TimeUnit.SECONDS);
    }

    @Override
    public TokenContext getUserStatus(String authenticationToken)
            throws KustvaktException {
        jlog.debug("retrieving user session for user '{}'",
                authenticationToken);
        if (authenticationToken == null)
            throw new KustvaktException(StatusCodes.PERMISSION_DENIED);
        return this.sessions.getSession(authenticationToken);
    }

    @Override
    public TokenContext createUserSession(User user, Map attr)
            throws KustvaktException {
        DateTime now = TimeUtils.getNow();
        DateTime ex = TimeUtils
                .getExpiration(now.getMillis(), config.getExpiration());
        String token = crypto
                .createToken(true, user.getUsername(), now.getMillis());
        TokenContext ctx = new TokenContext();
        ctx.setUsername(user.getUsername());
        ctx.setTokenType(Attributes.SESSION_AUTHENTICATION);
        ctx.setToken(token);
        ctx.setExpirationTime(ex.getMillis());
        ctx.setHostAddress(attr.get(Attributes.HOST).toString());
        ctx.setUserAgent(attr.get(Attributes.USER_AGENT).toString());
        this.sessions.putSession(token, ctx);
        jlog.info("create session for user: " + user.getUsername());
        return ctx;
    }

    @Override
    public void removeUserSession(String token) {
        this.sessions.removeSession(token);
    }

    @Override
    public TokenContext refresh(TokenContext context) throws KustvaktException {
        throw new UnsupportedOperationException("method not supported");
    }

    @Override
    public String getIdentifier() {
        return Attributes.SESSION_AUTHENTICATION;
    }

}
