package de.ids_mannheim.korap.security.auth;

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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * session object to hold current user sessions and track inactive time to close
 * unused sessions. Inactive sessions are not enforced until user makes a
 * request through thrift
 *
 * @author hanl
 */
public class SessionFactory implements Runnable {

    private static Logger jlog = LoggerFactory.getLogger(SessionFactory.class);

    private final ConcurrentMap<String, TokenContext> sessionsObject;
    private final ConcurrentMap<String, DateTime> timeCheck;
    private final ConcurrentMultiMap<String, String> loggedInRecord;
    //    private final ConcurrentMultiMap<String, Long> failedLogins;
    private final boolean multipleEnabled;
    private final int inactive;

    public SessionFactory(boolean multipleEnabled, int inactive) {
        jlog.debug("allow multiple sessions per user: '{}'", multipleEnabled);
        this.multipleEnabled = multipleEnabled;
        this.inactive = inactive;
        this.sessionsObject = new ConcurrentHashMap<>();
        this.timeCheck = new ConcurrentHashMap<>();
        this.loggedInRecord = new ConcurrentMultiMap<>();
    }

    public boolean hasSession(TokenContext context) {
        if (context.getUsername().equalsIgnoreCase(DemoUser.DEMOUSER_NAME))
            return false;
        if (loggedInRecord.containsKey(context.getUsername()) && !loggedInRecord
                .get(context.getUsername()).isEmpty())
            return true;
        return false;
    }

    @Cacheable("session")
    public TokenContext getSession(String token) throws KustvaktException {
        jlog.debug("logged in users: {}", loggedInRecord);
        TokenContext context = sessionsObject.get(token);
        if (context != null) {
            if (isUserSessionValid(token)) {
                resetInterval(token);
                return context;
            }else
                throw new KustvaktException(StatusCodes.EXPIRED);

        }else
            throw new KustvaktException(StatusCodes.PERMISSION_DENIED);
    }

    //todo: ?!
    @CacheEvict(value = "session", key = "#session.token")
    public void putSession(final String token, final TokenContext activeUser)
            throws KustvaktException {
        if (!hasSession(activeUser) | multipleEnabled) {
            loggedInRecord.put(activeUser.getUsername(), token);
            sessionsObject.put(token, activeUser);
            timeCheck.put(token, TimeUtils.getNow());
        }else {
            removeAll(activeUser);
            throw new KustvaktException(StatusCodes.ALREADY_LOGGED_IN);
        }
    }

    public void removeAll(final TokenContext activeUser) {
        for (String existing : loggedInRecord.get(activeUser.getUsername())) {
            timeCheck.remove(existing);
            sessionsObject.remove(existing);
        }
        loggedInRecord.remove(activeUser.getUsername());
    }

    @CacheEvict(value = "session", key = "#session.token")
    public void removeSession(String token) {
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
    private void resetInterval(String token) {
        timeCheck.put(token, TimeUtils.getNow());
    }

    /**
     * if user possesses a valid non-expired session token
     *
     * @param token
     * @return validity of user to request a backend function
     */
    private boolean isUserSessionValid(String token) {
        if (timeCheck.containsKey(token)) {
            if (TimeUtils.plusSeconds(timeCheck.get(token).getMillis(),
                    inactive).isAfterNow()) {
                jlog.debug("user has session");
                return true;
            }else
                jlog.debug("user with token {} has an invalid session", token);
        }
        return false;
    }

    /**
     * clean inactive sessions from session object
     * TODO: persist userdata to database when session times out!
     */
    private void timeoutMaintenance() {
        jlog.debug("running session cleanup thread");
        Set<String> inactive = new HashSet<>();
        for (Entry<String, DateTime> entry : timeCheck.entrySet()) {
            if (!isUserSessionValid(entry.getKey())) {
                TokenContext user = sessionsObject.get(entry.getKey());
                jlog.debug("removing user session for user {}",
                        user.getUsername());
                inactive.add(user.getUsername());
                removeSession(entry.getKey());
            }
        }
        if (inactive.size() > 0)
            jlog.debug("removing inactive user session for users '{}' ",
                    inactive);

        //        keys:
        //        for (String key : failedLogins.getKeySet()) {
        //            DateTime d = new DateTime(failedLogins.get(key).get(1));
        //            if (d.isBeforeNow()) {
        //                failedLogins.remove(key);
        //                jlog.info("removed failed login counts due to expiration for user {}", key);
        //                continue keys;
        //            }
        //        }
    }

    /**
     * run cleanup-thread
     */
    @Override
    public void run() {
        timeoutMaintenance();
        jlog.debug("logged users: {}", loggedInRecord.toString());

    }
}
