package de.ids_mannheim.korap.authentication;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hanl
 * @date 11/11/2014
 */
public class LoginCounter {

    private static Logger jlog = LoggerFactory.getLogger(LoginCounter.class);
    private final Map<String, Long[]> failedLogins;
    private KustvaktConfiguration config;


    public LoginCounter (KustvaktConfiguration config) {
        jlog.debug("init login counter for authentication management");
        this.config = config;
        this.failedLogins = new HashMap<>();
    }


    public void resetFailedCounter (String username) {
        failedLogins.remove(username);
    }


    public void registerFail (String username) {
        long expires = TimeUtils.plusSeconds(config.getLoginAttemptTTL())
                .getMillis();
        long fail = 1;
        Long[] set = failedLogins.get(username);
        if (set != null)
            fail = set[0] + 1;
        else
            set = new Long[2];
        set[0] = fail;
        set[1] = expires;

        failedLogins.put(username, set);
        jlog.warn("user failed to login ({}) ",
                Arrays.asList(failedLogins.get(username)));
    }


    public boolean validate (String username) {
        Long[] set = failedLogins.get(username);
        if (set != null) {
            if (TimeUtils.isExpired(set[1])) {
                failedLogins.remove(username);
                return true;
            }
            else if (set[0] < config.getLoginAttemptNum())
                return true;
            return false;
        }
        return true;
    }

}
