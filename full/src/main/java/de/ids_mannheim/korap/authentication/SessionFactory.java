package de.ids_mannheim.korap.authentication;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.DemoUser;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.ConcurrentMultiMap;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * session object to hold current user sessions and track inactive
 * time to close
 * unused sessions. Inactive sessions are not enforced until user
 * makes a
 * request through thrift
 * 
 * @author hanl
 */
//todo: use simple ehcache!
public class SessionFactory implements Runnable {

    private static Logger jlog = LoggerFactory.getLogger(SessionFactory.class);

    public static ConcurrentMap<String, TokenContext> sessionsObject;
    public static ConcurrentMap<String, DateTime> timeCheck;
    public static ConcurrentMultiMap<String, String> loggedInRecord;
    //    private final ConcurrentMultiMap<String, Long> failedLogins;
    private final boolean multipleEnabled;
    private final int inactive;


    public SessionFactory (boolean multipleEnabled, int inactive) {
        jlog.debug("allow multiple sessions per user: '{}'", multipleEnabled);
        this.multipleEnabled = multipleEnabled;
        this.inactive = inactive;
        this.sessionsObject = new ConcurrentHashMap<>();
        this.timeCheck = new ConcurrentHashMap<>();
        this.loggedInRecord = new ConcurrentMultiMap<>();
    }


    public boolean hasSession (TokenContext context) {
        if (context.getUsername().equalsIgnoreCase(DemoUser.DEMOUSER_NAME))
            return false;

        List<String> value = loggedInRecord.get(context.getUsername());
        return value != null && !value.isEmpty();
    }

    // todo: remove this!
    @Cacheable("session")
    public TokenContext getSession (String token) throws KustvaktException {
        jlog.debug("logged in users: {}", loggedInRecord);
        TokenContext context = sessionsObject.get(token);
        if (context != null) {
            // fixme: set context to respecitve expiratin interval and return context. handler checks expiration later!
            if (isUserSessionValid(token)) {
                resetInterval(token);
            }
            else
                throw new KustvaktException(StatusCodes.EXPIRED);

        }
         return context;
    }


    //todo: ?!
    @CacheEvict(value = "session", key = "#session.token")
    public void putSession (final String token, final TokenContext activeUser)
            throws KustvaktException {
        if (!hasSession(activeUser) | multipleEnabled) {
            loggedInRecord.put(activeUser.getUsername(), token);
            sessionsObject.put(token, activeUser);
            timeCheck.put(token, TimeUtils.getNow());
        }
        else {
            removeAll(activeUser);
            throw new KustvaktException(StatusCodes.ALREADY_LOGGED_IN);
        }
    }


    public void removeAll (final TokenContext activeUser) {
        for (String existing : loggedInRecord.get(activeUser.getUsername())) {
            timeCheck.remove(existing);
            sessionsObject.remove(existing);
        }
        loggedInRecord.remove(activeUser.getUsername());
    }


    @CacheEvict(value = "session", key = "#session.token")
    public void removeSession (String token) {
        String username = sessionsObject.get(token).getUsername();
        loggedInRecord.remove(username, token);
        if (loggedInRecord.get(username).isEmpty())
            loggedInRecord.remove(username);
        timeCheck.remove(token);
        sessionsObject.remove(token);
    }


    /**
     * reset inactive time interval to 0
     * 
     * @param token
     */
    private void resetInterval (String token) {
        timeCheck.put(token, TimeUtils.getNow());
    }


    /**
     * if user possesses a valid non-expired session token
     * 
     * @param token
     * @return validity of user to request a backend function
     */
    private boolean isUserSessionValid (String token) {
        if (timeCheck.containsKey(token)) {
            if (TimeUtils.plusSeconds(timeCheck.get(token).getMillis(),
                    inactive).isAfterNow()) {
                jlog.debug("user has session");
                return true;
            }
            else
                jlog.debug("user with token {} has an invalid session", token);
        }
        return false;
    }


    /**
     * clean inactive sessions from session object
     * TODO: persist userdata to database when session times out!
     */
    private void timeoutMaintenance () {
        jlog.trace("running session cleanup thread");
        Set<String> inactive = new HashSet<>();
        for (Entry<String, DateTime> entry : timeCheck.entrySet()) {
            if (!isUserSessionValid(entry.getKey())) {
                TokenContext user = sessionsObject.get(entry.getKey());
                jlog.trace("removing user session for user {}",
                        user.getUsername());
                inactive.add(user.getUsername());
                removeSession(entry.getKey());
            }
        }
        // fixme: not doing anything!
        if (inactive.size() > 0)
            jlog.trace("removing inactive user session for users '{}' ",
                    inactive);
    }


    /**
     * run cleanup-thread
     */
    @Override
    public void run () {
        timeoutMaintenance();
        if (loggedInRecord.size() > 0)
            jlog.debug("logged users: {}", loggedInRecord.toString());
    }
}
