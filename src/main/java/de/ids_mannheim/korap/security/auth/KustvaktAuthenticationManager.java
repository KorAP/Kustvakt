package de.ids_mannheim.korap.security.auth;

import de.ids_mannheim.korap.auditing.AuditRecord;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.exceptions.*;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.StringUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * contains the logic to authentication and registration processes. Uses
 * interface implementations (AuthenticationIface) for different databases and handlers
 *
 * @author hanl
 */
public class KustvaktAuthenticationManager extends AuthenticationManagerIface {

    private static String KEY = "kustvakt:key";
    private static Logger jlog = LoggerFactory
            .getLogger(KustvaktAuthenticationManager.class);
    private EncryptionIface crypto;
    private EntityHandlerIface entHandler;
    private AuditingIface auditing;
    private KustvaktConfiguration config;
    private final LoginCounter counter;
    private Cache user_cache;

    public KustvaktAuthenticationManager(EntityHandlerIface userdb,
            EncryptionIface crypto, KustvaktConfiguration config,
            AuditingIface auditer) {
        this.entHandler = userdb;
        this.config = config;
        this.crypto = crypto;
        this.auditing = auditer;
        this.counter = new LoginCounter(config);
        this.user_cache = CacheManager.getInstance().getCache("users");
    }

    /**
     * get session object if token was a session token
     *
     * @param token
     * @param host
     * @param useragent
     * @return
     * @throws KustvaktException
     */
    public TokenContext getTokenStatus(String token, String host,
            String useragent) throws KustvaktException {
        jlog.info("getting session status of token '{}'", token);
        AuthenticationIface provider = getProvider(
                StringUtils.getTokenType(token), null);

        if (provider == null)
            // throw exception for missing type paramter
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "token type not defined or found", "token_type");

        TokenContext context = provider.getUserStatus(token);
        //        if (!matchStatus(host, useragent, context))
        //            provider.removeUserSession(token);
        return context;
    }

    public User getUser(String username) throws KustvaktException {
        User user;
        String key = cache_key(username);
        Element e = user_cache.get(key);

        if (e != null) {
            Map map = (Map) e.getObjectValue();
            user = User.UserFactory.toUser(map);
        }else {
            user = entHandler.getAccount(username);
            user_cache.put(new Element(key, user.toCache()));
            // todo: not valid. for the duration of the session, the host should not change!
        }
        //todo:
        //        user.addField(Attributes.HOST, context.getHostAddress());
        //        user.addField(Attributes.USER_AGENT, context.getUserAgent());
        return user;
    }

    public TokenContext refresh(TokenContext context) throws KustvaktException {
        AuthenticationIface provider = getProvider(context.getTokenType(),
                null);
        if (provider == null) {
            //todo:
        }

        try {
            provider.removeUserSession(context.getToken());
            User user = getUser(context.getUsername());
            return provider.createUserSession(user, context.params());
        }catch (KustvaktException e) {
            throw new WrappedException(e, StatusCodes.LOGIN_FAILED);
        }
    }

    /**
     * @param type
     * @param attributes contains username and password to authenticate the user.
     *                   Depending of the authentication schema, may contain other values as well
     * @return User
     * @throws KustvaktException
     */
    @Override
    public User authenticate(int type, String username, String password,
            Map<String, Object> attributes) throws KustvaktException {
        User user;
        switch (type) {
            case 1:
                // todo:
                user = authenticateShib(attributes);
                break;
            default:
                user = authenticate(username, password, attributes);
                break;
        }
        auditing.audit(AuditRecord
                .serviceRecord(user.getId(), StatusCodes.LOGIN_SUCCESSFUL,
                        user.toString()));
        return user;
    }

    // todo: dont use annotations for caching
    @Override
    @CachePut(value = "users", key = "#user.getUsername()")
    public TokenContext createTokenContext(User user, Map<String, Object> attr,
            String provider_key) throws KustvaktException {
        AuthenticationIface provider = getProvider(provider_key,
                Attributes.API_AUTHENTICATION);

        if (attr.get(Attributes.SCOPES) != null)
            this.getUserData(user, UserDetails.class);

        TokenContext context = provider.createUserSession(user, attr);
        if (context == null)
            throw new KustvaktException(StatusCodes.NOT_SUPPORTED);
        context.setUserAgent((String) attr.get(Attributes.USER_AGENT));
        context.setHostAddress(Attributes.HOST);
        return context;
    }

    //todo: test
    @Deprecated
    private boolean matchStatus(String host, String useragent,
            TokenContext context) {
        if (host.equals(context.getHostAddress())) {
            if (useragent.equals(context.getUserAgent()))
                return true;
        }
        return false;
    }

    private User authenticateShib(Map<String, Object> attributes)
            throws KustvaktException {
        // todo use persistent id, since eppn is not unique
        String eppn = (String) attributes.get(Attributes.EPPN);

        if (eppn == null || eppn.isEmpty())
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);

        if (!attributes.containsKey(Attributes.EMAIL)
                && crypto.validateEntry(eppn, Attributes.EMAIL) != null)
            attributes.put(Attributes.EMAIL, eppn);

        // fixme?!
        User user = isRegistered(eppn);
        if (user == null)
            user = createShibbUserAccount(attributes);
        return user;
    }

    //todo: what if attributes null?
    private User authenticate(String username, String password,
            Map<String, Object> attr) throws KustvaktException {
        Map<String, Object> attributes = crypto.validateMap(attr);
        String safeUS;
        User unknown;
        // just to make sure that the plain password does not appear anywhere in the logs!

        try {
            safeUS = crypto.validateEntry(username, Attributes.USERNAME);
        }catch (KustvaktException e) {
            throw new WrappedException(e, StatusCodes.LOGIN_FAILED, username);
        }

        if (safeUS == null || safeUS.isEmpty())
            throw new WrappedException(new KustvaktException(username,
                    StatusCodes.BAD_CREDENTIALS), StatusCodes.LOGIN_FAILED);
        else {
            try {
                unknown = entHandler.getAccount(safeUS);
            }catch (EmptyResultException e) {
                // mask exception to disable user guessing in possible attacks
                throw new WrappedException(new KustvaktException(username,
                        StatusCodes.BAD_CREDENTIALS), StatusCodes.LOGIN_FAILED,
                        username);
            }catch (KustvaktException e) {
                throw new WrappedException(e, StatusCodes.LOGIN_FAILED,
                        attributes.toString());
            }
        }
        jlog.trace("Authentication: found username " + unknown.getUsername());
        if (unknown instanceof KorAPUser) {
            if (password == null || password.isEmpty())
                throw new WrappedException(
                        new KustvaktException(unknown.getId(),
                                StatusCodes.BAD_CREDENTIALS),
                        StatusCodes.LOGIN_FAILED, username);

            KorAPUser user = (KorAPUser) unknown;
            boolean check = crypto.checkHash(password, user.getPassword());

            if (!check) {
                // the fail counter only applies for wrong password
                jlog.warn("Wrong Password!");
                processLoginFail(unknown);
                throw new WrappedException(new KustvaktException(user.getId(),
                        StatusCodes.BAD_CREDENTIALS), StatusCodes.LOGIN_FAILED,
                        username);
            }

            // bad credentials error has presedence over account locked or unconfirmed codes
            // since latter can lead to account guessing of third parties
            if (user.isAccountLocked()) {
                URIParam param = (URIParam) user.getField(URIParam.class);

                if (param.hasValues()) {
                    jlog.debug("Account is not yet activated for user '{}'",
                            user.getUsername());
                    if (TimeUtils.getNow().isAfter(param.getUriExpiration())) {
                        jlog.error(
                                "URI token is expired. Deleting account for user {}",
                                user.getUsername());
                        deleteAccount(user);
                        throw new WrappedException(
                                new KustvaktException(unknown.getId(),
                                        StatusCodes.EXPIRED,
                                        "account confirmation uri has expired",
                                        param.getUriFragment()),
                                StatusCodes.LOGIN_FAILED, username);
                    }
                    throw new WrappedException(
                            new KustvaktException(unknown.getId(),
                                    StatusCodes.UNCONFIRMED_ACCOUNT),
                            StatusCodes.LOGIN_FAILED, username);
                }
                jlog.error("ACCESS DENIED: account not active for '{}'",
                        unknown.getUsername());
                throw new WrappedException(
                        new KustvaktException(unknown.getId(),
                                StatusCodes.ACCOUNT_DEACTIVATED),
                        StatusCodes.LOGIN_FAILED, username);
            }

        }else if (unknown instanceof ShibUser) {
            //todo
        }
        jlog.debug("Authentication done: " + safeUS);
        return unknown;
    }

    public User isRegistered(String username) throws KustvaktException {
        User user;
        if (username == null || username.isEmpty())
            throw new KustvaktException(username, StatusCodes.ILLEGAL_ARGUMENT,
                    "username must be set", username);

        try {
            user = entHandler.getAccount(username);
        }catch (EmptyResultException e) {
            jlog.debug("user does not exist ({})", username);
            return null;

        }catch (KustvaktException e) {
            jlog.error("KorAPException", e);
            throw new KustvaktException(username, StatusCodes.ILLEGAL_ARGUMENT,
                    "username invalid", username);
        }
        return user;
    }

    public void logout(TokenContext context) throws KustvaktException {
        String key = cache_key(context.getUsername());
        try {
            AuthenticationIface provider = getProvider(context.getTokenType(),
                    null);

            if (provider == null) {
                //todo:
                return;
            }
            provider.removeUserSession(context.getToken());
        }catch (KustvaktException e) {
            throw new WrappedException(e, StatusCodes.LOGOUT_FAILED,
                    context.toString());
        }
        auditing.audit(AuditRecord.serviceRecord(context.getUsername(),
                StatusCodes.LOGOUT_SUCCESSFUL, context.toString()));
        user_cache.remove(key);
    }

    private void processLoginFail(User user) throws KustvaktException {
        counter.registerFail(user.getUsername());
        if (!counter.validate(user.getUsername())) {
            try {
                this.lockAccount(user);
            }catch (KustvaktException e) {
                jlog.error("user account could not be locked!", e);
                throw new WrappedException(e,
                        StatusCodes.UPDATE_ACCOUNT_FAILED);
            }
            throw new WrappedException(new KustvaktException(user.getId(),
                    StatusCodes.ACCOUNT_DEACTIVATED), StatusCodes.LOGIN_FAILED);
        }
    }

    public void lockAccount(User user) throws KustvaktException {
        if (!(user instanceof KorAPUser))
            throw new KustvaktException(StatusCodes.REQUEST_INVALID);

        KorAPUser u = (KorAPUser) user;
        u.setAccountLocked(true);
        jlog.info("locking account for user: {}", user.getUsername());
        entHandler.updateAccount(u);
    }

    public KorAPUser checkPasswordAllowance(KorAPUser user, String oldPassword,
            String newPassword) throws KustvaktException {
        String dbPassword = user.getPassword();

        if (oldPassword.trim().equals(newPassword.trim())) {
            // TODO: special error StatusCodes for this?
            throw new WrappedException(new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT),
                    StatusCodes.PASSWORD_RESET_FAILED, newPassword);
        }

        boolean check = crypto.checkHash(oldPassword, dbPassword);

        if (!check)
            throw new WrappedException(new KustvaktException(user.getId(),
                    StatusCodes.BAD_CREDENTIALS),
                    StatusCodes.PASSWORD_RESET_FAILED);

        try {
            user.setPassword(crypto.produceSecureHash(newPassword));
        }catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            //            throw new KorAPException(StatusCodes.ILLEGAL_ARGUMENT,
            //                    "Creating password hash failed!", "password");
            throw new WrappedException(new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "password invalid",
                    newPassword), StatusCodes.PASSWORD_RESET_FAILED,
                    user.toString(), newPassword);
        }
        return user;
    }

    //fixme: use clientinfo for logging/auditing?! = from where did he access the reset function?
    @Override
    public void resetPassword(String uriFragment, String username,
            String newPassphrase) throws KustvaktException {
        String safeUser, safePass;

        try {
            safeUser = crypto.validateEntry(username, Attributes.USERNAME);
            safePass = crypto.validateEntry(newPassphrase, Attributes.PASSWORD);
        }catch (KustvaktException e) {
            jlog.error("Error", e);
            throw new WrappedException(new KustvaktException(username,
                    StatusCodes.ILLEGAL_ARGUMENT, "password invalid",
                    newPassphrase), StatusCodes.PASSWORD_RESET_FAILED, username,
                    newPassphrase);
        }

        try {
            safePass = crypto.produceSecureHash(safePass);
        }catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            jlog.error("Encoding/Algorithm Error", e);
            throw new WrappedException(new KustvaktException(username,
                    StatusCodes.ILLEGAL_ARGUMENT, "password invalid",
                    newPassphrase), StatusCodes.PASSWORD_RESET_FAILED, username,
                    uriFragment, newPassphrase);
        }
        int result = entHandler
                .resetPassphrase(safeUser, uriFragment, safePass);

        if (result == 0)
            throw new WrappedException(
                    new KustvaktException(username, StatusCodes.EXPIRED,
                            "URI fragment expired", uriFragment),
                    StatusCodes.PASSWORD_RESET_FAILED, username, uriFragment);
        else if (result == 1)
            jlog.info("successfully reset password for user {}", safeUser);
    }

    public void confirmRegistration(String uriFragment, String username)
            throws KustvaktException {
        String safeUser;
        try {
            safeUser = crypto.validateEntry(username, Attributes.USERNAME);
        }catch (KustvaktException e) {
            jlog.error("error", e);
            throw new WrappedException(e,
                    StatusCodes.ACCOUNT_CONFIRMATION_FAILED, username,
                    uriFragment);
        }
        int r = entHandler.activateAccount(safeUser, uriFragment);
        if (r == 0) {
            User user;
            try {
                user = entHandler.getAccount(username);
            }catch (EmptyResultException e) {
                throw new WrappedException(new KustvaktException(username,
                        StatusCodes.BAD_CREDENTIALS),
                        StatusCodes.ACCOUNT_CONFIRMATION_FAILED, username,
                        uriFragment);
            }
            entHandler.deleteAccount(user.getId());
            throw new WrappedException(
                    new KustvaktException(user.getId(), StatusCodes.EXPIRED),
                    StatusCodes.ACCOUNT_CONFIRMATION_FAILED, username,
                    uriFragment);
        }else if (r == 1)
            jlog.info("successfully confirmed user registration for user {}",
                    safeUser);
        // register successful audit!
    }

    /**
     * @param attributes
     * @return
     * @throws KustvaktException
     */
    //fixme: remove clientinfo object (not needed), use json representation to get stuff
    public User createUserAccount(Map<String, Object> attributes,
            boolean confirmation_required) throws KustvaktException {
        Map<String, Object> safeMap = crypto.validateMap(attributes);
        if (safeMap.get(Attributes.USERNAME) == null || ((String) safeMap
                .get(Attributes.USERNAME)).isEmpty())
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "username must be set", "username");
        if (safeMap.get(Attributes.PASSWORD) == null || ((String) safeMap
                .get(Attributes.PASSWORD)).isEmpty())
            throw new KustvaktException(safeMap.get(Attributes.USERNAME),
                    StatusCodes.ILLEGAL_ARGUMENT, "password must be set",
                    "password");

        String username = crypto
                .validateEntry((String) safeMap.get(Attributes.USERNAME),
                        Attributes.USERNAME);
        String safePass = crypto
                .validateEntry((String) safeMap.get(Attributes.PASSWORD),
                        Attributes.PASSWORD);
        String hash;
        try {
            hash = crypto.produceSecureHash(safePass);
        }catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            jlog.error("Encryption error", e);
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT);
        }

        KorAPUser user = User.UserFactory.getUser(username);
        user.setAccountLocked(confirmation_required);

        if (confirmation_required) {
            URIParam param = new URIParam(crypto.createToken(),
                    TimeUtils.plusSeconds(config.getExpiration()).getMillis());
            user.addField(param);
        }
        user.setPassword(hash);
        try {
            entHandler.createAccount(user);
            UserDetails details = new UserDetails(user.getId());
            details.readDefaults(safeMap);
            details.checkRequired();

            UserSettings settings = new UserSettings(user.getId());
            settings.readDefaults(safeMap);
            settings.checkRequired();

            UserdataFactory.getDaoInstance(UserDetails.class).store(details);
            UserdataFactory.getDaoInstance(UserSettings.class).store(settings);
        }catch (KustvaktException e) {
            throw new WrappedException(e, StatusCodes.CREATE_ACCOUNT_FAILED,
                    user.toString());
        }

        auditing.audit(AuditRecord.serviceRecord(user.getUsername(),
                StatusCodes.CREATE_ACCOUNT_SUCCESSFUL));
        return user;
    }

    //todo:
    private ShibUser createShibbUserAccount(Map<String, Object> attributes)
            throws KustvaktException {
        jlog.debug("creating shibboleth user account for user attr: {}",
                attributes);
        Map<String, Object> safeMap = crypto.validateMap(attributes);

        //todo eppn non-unique.join with idp or use persistent_id as username identifier
        ShibUser user = User.UserFactory
                .getShibInstance((String) safeMap.get(Attributes.EPPN),
                        (String) safeMap.get(Attributes.MAIL),
                        (String) safeMap.get(Attributes.CN));
        user.setAffiliation((String) safeMap.get(Attributes.EDU_AFFIL));
        user.setAccountCreation(TimeUtils.getNow().getMillis());
        entHandler.createAccount(user);

        UserDetails d = new UserDetails(user.getId());
        d.readDefaults(attributes);
        d.checkRequired();

        UserdataFactory.getDaoInstance(d.getClass()).store(d);

        UserSettings s = new UserSettings(user.getId());
        s.readDefaults(attributes);
        s.checkRequired();
        UserdataFactory.getDaoInstance(s.getClass()).store(s);

        return user;
    }

    /**
     * link shibboleth and korap user account to one another.
     *
     * @param current    currently logged in user
     * @param for_name   foreign user name the current account should be linked to
     * @param transstrat transfer status of user data (details, settings, user queries)
     *                   0 = the currently logged in data should be kept
     *                   1 = the foreign account data should be kept
     * @throws NotAuthorizedException
     * @throws KustvaktException
     */
    // todo:
    public void accountLink(User current, String for_name, int transstrat)
            throws KustvaktException {
        //        User foreign = entHandler.getAccount(for_name);

        //        if (current.getAccountLink() == null && current.getAccountLink()
        //                .isEmpty()) {
        //            if (current instanceof KorAPUser && foreign instanceof ShibUser) {
        //                if (transstrat == 1)
        //                    current.transfer(foreign);
        ////                foreign.setAccountLink(current.getUsername());
        ////                current.setAccountLink(foreign.getUsername());
        //                //                entHandler.purgeDetails(foreign);
        //                //                entHandler.purgeSettings(foreign);
        //            }else if (foreign instanceof KorAPUser
        //                    && current instanceof ShibUser) {
        //                if (transstrat == 0)
        //                    foreign.transfer(current);
        ////                current.setAccountLink(foreign.getUsername());
        //                //                entHandler.purgeDetails(current);
        //                //                entHandler.purgeSettings(current);
        //                //                entHandler.purgeSettings(current);
        //            }
        //        entHandler.updateAccount(current);
        //        entHandler.updateAccount(foreign);
        //        }
    }

    public boolean updateAccount(User user) throws KustvaktException {
        boolean result;
        String key = cache_key(user.getUsername());
        if (user instanceof DemoUser)
            throw new KustvaktException(user.getId(),
                    StatusCodes.REQUEST_INVALID,
                    "account not updateable for demo user", user.getUsername());
        else {
            //            crypto.validate(user);
            try {
                result = entHandler.updateAccount(user) > 0;
            }catch (KustvaktException e) {
                jlog.error("Error ", e);
                throw new WrappedException(e,
                        StatusCodes.UPDATE_ACCOUNT_FAILED);
            }
        }
        if (result) {
            user_cache.remove(key);
            auditing.audit(AuditRecord.serviceRecord(user.getId(),
                    StatusCodes.UPDATE_ACCOUNT_SUCCESSFUL, user.toString()));
        }
        return result;
    }

    public boolean deleteAccount(User user) throws KustvaktException {
        boolean result;
        String key = cache_key(user.getUsername());
        if (user instanceof DemoUser)
            return true;
        else {
            try {
                result = entHandler.deleteAccount(user.getId()) > 0;
            }catch (KustvaktException e) {
                jlog.error("Error ", e);
                throw new WrappedException(e,
                        StatusCodes.DELETE_ACCOUNT_FAILED);
            }
        }
        if (result) {
            user_cache.remove(key);
            auditing.audit(AuditRecord.serviceRecord(user.getUsername(),
                    StatusCodes.DELETE_ACCOUNT_SUCCESSFUL, user.toString()));
        }
        return result;
    }

    public Object[] validateResetPasswordRequest(String username, String email)
            throws KustvaktException {
        String mail, uritoken;
        mail = crypto.validateEntry(email, Attributes.EMAIL);
        User ident;
        try {
            ident = entHandler.getAccount(username);
            if (ident instanceof DemoUser)
                //            throw new NotAuthorizedException(StatusCodes.PERMISSION_DENIED,
                //                    "password reset now allowed for DemoUser", "");
                throw new WrappedException(username,
                        StatusCodes.PASSWORD_RESET_FAILED, username);
        }catch (EmptyResultException e) {
            throw new WrappedException(new KustvaktException(username,
                    StatusCodes.ILLEGAL_ARGUMENT, "username not found",
                    username), StatusCodes.PASSWORD_RESET_FAILED, username);
        }

        Userdata data = this.getUserData(ident, UserDetails.class);
        KorAPUser user = (KorAPUser) ident;

        if (!mail.equals(data.get(Attributes.EMAIL)))
            //            throw new NotAuthorizedException(StatusCodes.ILLEGAL_ARGUMENT,
            //                    "invalid parameter: email", "email");
            throw new WrappedException(new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "email invalid", email),
                    StatusCodes.PASSWORD_RESET_FAILED, email);
        uritoken = crypto.encodeBase();
        URIParam param = new URIParam(uritoken,
                TimeUtils.plusHours(24).getMillis());
        user.addField(param);

        try {
            entHandler.updateAccount(user);
        }catch (KustvaktException e) {
            jlog.error("Error ", e);
            throw new WrappedException(e, StatusCodes.PASSWORD_RESET_FAILED);
        }
        return new Object[] { uritoken,
                new DateTime(param.getUriExpiration()) };
    }

    @Override
    public <T extends Userdata> T getUserData(User user, Class<T> clazz)
            throws WrappedException {

        try {
            UserDataDbIface<T> dao = UserdataFactory.getDaoInstance(clazz);
            T data = dao.get(user);
            if (data == null)
                throw new WrappedException(user.getId(),
                        StatusCodes.EMPTY_RESULTS, clazz.getSimpleName());

            return data;
        }catch (KustvaktException e) {
            jlog.error("Error ", e);
            throw new WrappedException(e, StatusCodes.GET_ACCOUNT_FAILED);
        }
    }

    //todo: cache userdata outside of the user object!
    @Override
    public void updateUserData(Userdata data) throws WrappedException {
        try {
            data.validate(this.crypto);
            UserDataDbIface dao = UserdataFactory
                    .getDaoInstance(data.getClass());
            dao.update(data);
        }catch (KustvaktException e) {
            jlog.error("Error ", e);
            throw new WrappedException(e, StatusCodes.UPDATE_ACCOUNT_FAILED);
        }
    }

    //    public UserDetails getUserDetails(User user) throws KustvaktException {
    //        try {
    //            if (user.getDetails() == null)
    //                user.setDetails(entHandler.getUserDetails(user.getId()));
    //        }catch (KustvaktException e) {
    //            throw new WrappedException(e, StatusCodes.GET_ACCOUNT_FAILED);
    //        }
    //        return user.getDetails();
    //    }
    //
    //    public UserSettings getUserSettings(User user) throws KustvaktException {
    //        try {
    //            if (user.getSettings() == null)
    //                user.setSettings(entHandler.getUserSettings(user.getId()));
    //        }catch (KustvaktException e) {
    //            throw new WrappedException(e, StatusCodes.GET_ACCOUNT_FAILED);
    //        }
    //        return user.getSettings();
    //    }

    private String cache_key(String input) throws KustvaktException {
        try {
            return crypto.hash(KEY + "@" + input);
        }catch (Exception e) {
            jlog.error("illegal cache key input '{}'", input);
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "missing or illegal cache key", input);
        }
    }
}
