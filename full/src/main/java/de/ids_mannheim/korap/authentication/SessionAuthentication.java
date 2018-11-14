package de.ids_mannheim.korap.authentication;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * implementation of the AuthenticationIface to handle korap
 * authentication
 * internals
 * 
 * @author hanl
 */
public class SessionAuthentication implements AuthenticationIface {

    private static final Logger jlog = LogManager
            .getLogger(SessionAuthentication.class);
    public static SessionFactory sessions;
    private ScheduledThreadPoolExecutor scheduled;
    private EncryptionIface crypto;
    private KustvaktConfiguration config;


    public SessionAuthentication (KustvaktConfiguration config,
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
    public TokenContext getTokenContext(String authenticationToken)
            throws KustvaktException {
        jlog.debug("retrieving user session for user "+ authenticationToken);
        return this.sessions.getSession(authenticationToken);
    }


    @Override
    public TokenContext createTokenContext(User user, Map<String, Object> attr)
            throws KustvaktException {
        DateTime now = TimeUtils.getNow();
        DateTime ex = TimeUtils.getExpiration(now.getMillis(),
                config.getShortTokenTTL());
        String token = crypto.createToken(true, user.getUsername(),
                now.getMillis());
        TokenContext ctx = new TokenContext();
        ctx.setUsername(user.getUsername());
        ctx.setTokenType(TokenType.SESSION);
        ctx.setToken(token);
        ctx.setExpirationTime(ex.getMillis()+(1000));
        ctx.setHostAddress(attr.get(Attributes.HOST).toString());
        ctx.setUserAgent(attr.get(Attributes.USER_AGENT).toString());
        jlog.debug(ctx.toJson());
        this.sessions.putSession(token, ctx);
        jlog.debug("session " +sessions.getSession(token).toString());
        jlog.info("create session for user: " + user.getUsername());
        return ctx;
    }


    @Override
    public void removeUserSession (String token) {
        this.sessions.removeSession(token);
    }


    @Override
    public TokenContext refresh (TokenContext context) throws KustvaktException {
        throw new UnsupportedOperationException("method not supported");
    }


    @Override
    public TokenType getTokenType () {
        return TokenType.SESSION;
    }

}
