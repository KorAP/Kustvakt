package de.ids_mannheim.korap.utils;

import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger jlog = LoggerFactory
            .getLogger(UserPropertyReader.class);


    public UserPropertyReader (String path) {
        this.path = path;
        this.iface = BeansFactory.getKustvaktContext().getUserDBHandler();
        this.crypto = BeansFactory.getKustvaktContext().getEncryption();
    }


    @Override
    public void load () {
        try {
            props = super.read(this.path);
            for (Map.Entry<String, Properties> e : props.entrySet()) {
                try {
                    createUser(e.getKey(), e.getValue());
                }
                catch (KustvaktException ex) {
                    jlog.error("KorAP-Exception: {} for user {}",
                            ex.getStatusCode(), e.getKey());
                }
            }
            iface.createAccount(User.UserFactory.getDemoUser());
        }
        catch (IOException e) {
            jlog.error("Could not read from path {}", path);
        }
        catch (KustvaktException e) {
            jlog.error("KorAP-Exception: {}", e.getStatusCode());
        }
    }


    private User createUser (String username, Properties p)
            throws KustvaktException {
        KorAPUser user;
        if (username.equals(User.ADMINISTRATOR_NAME)) {
            user = User.UserFactory.getAdmin();

            String pass = p.getProperty(username + ".password", null);
            if (pass == null)
                throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);

            try {
                pass = crypto.secureHash(pass);
            }
            catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new KustvaktException(StatusCodes.REQUEST_INVALID);
            }
            user.setPassword(pass);
            iface.createAccount(user);
        }
        else {
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
                pass = crypto.secureHash(pass);
            }
            catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new KustvaktException(StatusCodes.REQUEST_INVALID);
            }

            user.setPassword(pass);
            user.setAccountLocked(Boolean.valueOf(p.getProperty(username
                    + ".lock", "false")));
            user.setAccountCreation(TimeUtils.getNow().getMillis());

            //todo: make sure uri is set to 0, so sql queries work with the null value
            //            user.setURIExpiration(0L);
            iface.createAccount(user);
            UserDetails det = new UserDetails(user.getId());
            det.readDefaults(vals);
            det.validate(crypto);

            Userdata set = new UserSettings(user.getId());
            set.readDefaults(vals);
            set.validate(crypto);

            UserDataDbIface dao = BeansFactory.getTypeFactory().getTypedBean(
                    BeansFactory.getKustvaktContext().getUserDataDaos(),
                    UserDetails.class);
            dao.store(det);

            dao = BeansFactory.getTypeFactory().getTypedBean(
                    BeansFactory.getKustvaktContext().getUserDataDaos(),
                    UserSettings.class);
            dao.store(set);
        }

        jlog.info("successfully created account for user {}",
                user.getUsername());
        return user;
    }
}
