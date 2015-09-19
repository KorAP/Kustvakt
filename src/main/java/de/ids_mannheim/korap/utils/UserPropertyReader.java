package de.ids_mannheim.korap.utils;

import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.EntityHandlerIface;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserSettings;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author hanl
 * @date 30/09/2014
 */
public class UserPropertyReader extends PropertyReader {

    private Map<String, Properties> props;
    private String path;
    private EntityHandlerIface iface;
    private EncryptionIface crypto;
    private static Logger jlog = KustvaktLogger
            .initiate(UserPropertyReader.class);

    public UserPropertyReader(String path) {
        this.path = path;
        this.iface = BeanConfiguration.getBeans().getUserDBHandler();
        this.crypto = BeanConfiguration.getBeans().getEncryption();
    }

    @Override
    public void load() {
        try {
            props = super.read(this.path);
            for (Map.Entry<String, Properties> e : props.entrySet()) {
                try {
                    createUser(e.getKey(), e.getValue());
                }catch (KustvaktException ex) {
                    jlog.error("KorAP-Exception: {} for user {}",
                            ex.getStatusCode(), e.getKey());
                }
            }
            iface.createAccount(User.UserFactory.getDemoUser());
        }catch (IOException e) {
            jlog.error("Could not read from path {}", path);
        }catch (KustvaktException e) {
            jlog.error("KorAP-Exception: {}", e.getStatusCode());
        }
    }

    private User createUser(String username, Properties p)
            throws KustvaktException {
        KorAPUser user;
        if (username.equals(User.ADMINISTRATOR_NAME)) {
            user = User.UserFactory.getAdmin();

            String pass = p.getProperty(username + ".password", null);
            if (pass == null)
                throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);

            try {
                pass = crypto.produceSecureHash(pass);
            }catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new KustvaktException(StatusCodes.REQUEST_INVALID);
            }
            user.setPassword(pass);
        }else {
            user = User.UserFactory.getUser(username);
            Map<String, Object> vals = new HashMap<>();
            for (Map.Entry e : p.entrySet()) {
                String key = e.getKey().toString().split("\\.", 2)[1];
                vals.put(key, e.getValue().toString());
            }
            String pass = p.getProperty(username + ".password", null);
            if (pass == null)
                throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);

            try {
                pass = crypto.produceSecureHash(pass);
            }catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new KustvaktException(StatusCodes.REQUEST_INVALID);
            }

            user.setPassword(pass);
            user.setAccountLocked(Boolean.valueOf(
                    p.getProperty(username + ".lock", "false")));
            user.setAccountCreation(TimeUtils.getNow().getMillis());

            //todo: make sure uri is set to 0, so sql queries work with the null value
//            user.setURIExpiration(0L);

            UserDetails det = UserDetails.newDetailsIterator(vals);

            user.setDetails(det);
            user.setSettings(new UserSettings());
        }
        iface.createAccount(user);
        jlog.info("successfully created account for user {}",
                user.getUsername());
        return user;
    }
}
