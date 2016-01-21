package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.ParamFields;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.*;
import de.ids_mannheim.korap.utils.BooleanUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hanl
 * @date 13/01/2014
 */
public class EntityDao implements EntityHandlerIface {

    private static Logger jlog = LoggerFactory.getLogger(EntityDao.class);
    private NamedParameterJdbcTemplate jdbcTemplate;

    public EntityDao(PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }

    @Override
    public UserSettings getUserSettings(Integer userid)
            throws KustvaktException {
        //        TransactionDefinition def = new DefaultTransactionDefinition();
        //        TransactionStatus status = transactionManager.getTransaction(def);

        MapSqlParameterSource np = new MapSqlParameterSource();
        np.addValue("us", userid);
        final String sql =
                "SELECT user.* FROM user_settings as user inner join korap_users as a "
                        + "on a.id=user.user_id WHERE user.user_id=:us";
        try {
            Map s = this.jdbcTemplate.queryForMap(sql, np);
            return UserSettings.fromObjectMap(s);
        }catch (EmptyResultDataAccessException ex) {
            return new UserSettings();
        }catch (DataAccessException e) {
            jlog.error("Could not retrieve user settings for user: " + userid,
                    e);
            throw new dbException(userid, "user_settings",
                    StatusCodes.DB_GET_FAILED, userid.toString());
        }
    }

    @Override
    public int updateSettings(UserSettings settings) throws KustvaktException {
        final String sql =
                "UPDATE user_settings SET fileNameForExport=:fileNameForExport,"
                        +
                        "leftContextItemForExport=:leftContextItemForExport," +
                        "leftContextSizeForExport=:leftContextSizeForExport,locale=:locale,leftContextItem=:leftContextItem,"
                        +
                        "leftContextSize=:leftContextSize," +
                        "rightContextItem=:rightContextItem,rightContextItemForExport=:rightContextItemForExport,"
                        +
                        "rightContextSize=:rightContextSize," +
                        "POSFoundry=:defaultPOSfoundry, lemmaFoundry=:defaultLemmafoundry, constFoundry=:defaultConstfoundry, "
                        +
                        "relFoundry=:defaultRelfoundry, " +
                        "rightContextSizeForExport=:rightContextSizeForExport,selectedCollection=:selectedCollection,queryLanguage=:queryLanguage,"
                        +
                        "pageLength=:pageLength,metadataQueryExpertModus=:metadataQueryExpertModus,collectData=:collectData "
                        +
                        "WHERE user_id=:userID";
        try {
            return this.jdbcTemplate
                    .update(sql, new BeanPropertySqlParameterSource(settings));
        }catch (DataAccessException e) {
            jlog.error("Could not update user settings for user: " + settings
                    .getUserID(), e);
            throw new dbException(settings.getUserID(), "user_settings",
                    StatusCodes.DB_UPDATE_FAILED, settings.toString());
        }
    }

    /**
     * @param settings
     * @throws KustvaktException
     */

    private void createSettings(UserSettings settings)
            throws KustvaktException {
        final String sql =
                "INSERT INTO user_settings (user_id, fileNameForExport,leftContextItemForExport,"
                        +
                        "leftContextSizeForExport,locale,leftContextItem,leftContextSize,"
                        +
                        "rightContextItem,rightContextItemForExport,rightContextSize,"
                        +
                        "rightContextSizeForExport,selectedCollection,queryLanguage,"
                        +
                        "pageLength,metadataQueryExpertModus, POSFoundry, lemmaFoundry, constFoundry, relFoundry, collectData) "
                        +
                        "VALUES (:userID, :fileNameForExport, :leftContextItemForExport, "
                        +
                        ":leftContextSizeForExport, :locale, :leftContextItem, :leftContextSize, "
                        +
                        ":rightContextItem,:rightContextItemForExport, :rightContextSize, "
                        +
                        ":rightContextSizeForExport, :selectedCollection, :queryLanguage, "
                        +
                        ":pageLength, :metadataQueryExpertModus, :defaultPOSfoundry, "
                        +
                        ":defaultLemmafoundry, :defaultConstfoundry, :defaultRelfoundry, :collectData);";

        try {
            if (settings == null)
                throw new KustvaktException(StatusCodes.MISSING_ARGUMENTS,
                        "no settings provided", "user settings");
            this.jdbcTemplate
                    .update(sql, new BeanPropertySqlParameterSource(settings));
        }catch (DataAccessException e) {
            jlog.error("Could not create user settings for user: " + settings
                    .getUserID(), e);
            throw new dbException(settings.getUserID(), "userSettings",
                    StatusCodes.DB_INSERT_FAILED, settings.toString());
        }
    }

    @Override
    public UserDetails getUserDetails(Integer userid) throws KustvaktException {
        final String sql = "SELECT us.* FROM user_details as us inner join korap_users as ku on ku.id=us.user_id WHERE us.user_id=:user";
        MapSqlParameterSource np = new MapSqlParameterSource();
        np.addValue("user", userid);

        try {
            return this.jdbcTemplate
                    .queryForObject(sql, np, new RowMapper<UserDetails>() {
                        @Override
                        public UserDetails mapRow(ResultSet rs, int i)
                                throws SQLException {
                            UserDetails d = new UserDetails();
                            d.setUserID(rs.getInt("user_id"));
                            d.setAddress(rs.getString("address"));
                            d.setCountry(rs.getString("country"));
                            d.setEmail(rs.getString("email"));
                            d.setFirstName(rs.getString("firstName"));
                            d.setLastName(rs.getString("lastName"));
                            d.setGender(rs.getString("gender"));
                            d.setInstitution(rs.getString("institution"));
                            d.setPhone(rs.getString("phone"));
                            d.setPrivateUsage(rs.getBoolean("privateUsage"));
                            return d;
                        }
                    });
        }catch (EmptyResultDataAccessException ex) {
            //todo: create audit record?
            return null;
        }catch (DataAccessException e) {
            jlog.error("Could not retrieve user details for user: " + userid,
                    e);
            throw new dbException(userid, "userDetails",
                    StatusCodes.DB_GET_FAILED, userid.toString());
            //            throw new KorAPException(e, StatusCodes.CONNECTION_ERROR);
        }
    }

    @Override
    public int updateUserDetails(UserDetails details) throws KustvaktException {
        final String up =
                "UPDATE user_details SET firstName=:firstName, lastName=:lastName, "
                        +
                        "gender=:gender, phone=:phone, institution=:institution, "
                        +
                        "email=:email, address=:address, country=:country, privateUsage=:privateUsage "
                        +
                        "WHERE user_id=:userID;";
        try {
            return this.jdbcTemplate
                    .update(up, new BeanPropertySqlParameterSource(details));
        }catch (DataAccessException e) {
            jlog.error("Could not retrieve user details for user: " + details
                    .getUserID(), e);
            //            throw new KorAPException(e, StatusCodes.CONNECTION_ERROR);
            throw new dbException(details.getUserID(), "userDetails",
                    StatusCodes.DB_UPDATE_FAILED, details.toString());
        }
    }

    private void createUserDetails(UserDetails details)
            throws KustvaktException {
        final String up =
                "INSERT INTO user_details (user_id, firstName, lastName, gender, phone, institution, "
                        +
                        "email, address, country, privateUsage) VALUES (:userID, :firstName, :lastName, :gender, "
                        +
                        ":phone, :institution, :email, :address, :country, :privateUsage);";
        try {
            if (details == null)
                throw new KustvaktException(StatusCodes.MISSING_ARGUMENTS,
                        "no details provided", "user details");
            this.jdbcTemplate
                    .update(up, new BeanPropertySqlParameterSource(details));
        }catch (DataAccessException e) {
            jlog.error("Could not create user details for user: " + details
                    .getUserID(), e);
            throw new dbException(details.getUserID(), "userDetails",
                    StatusCodes.DB_INSERT_FAILED, details.toString());
        }
    }

    // usersettings are fetched plus basic account info, no details, since i rarely use them anyway!
    @Override
    public User getAccount(String username) throws KustvaktException {
        Map<String, String> namedParameters = Collections
                .singletonMap("username", username);
        final String sql = "select a.* from korap_users as a where a.username=:username;";
        User user;
        try {
            user = this.jdbcTemplate.queryForObject(sql, namedParameters,
                    new RowMapperFactory.UserMapper());
        }catch (EmptyResultDataAccessException ae) {
            jlog.error("No user found for name '{}'", username);
            throw new EmptyResultException(username);
        }catch (DataAccessException e) {
            jlog.error("Could not retrieve user for name: " + username, e);
            throw new dbException(username, "korap_users",
                    StatusCodes.DB_GET_FAILED, username);
        }
        return user;
    }

    @Override
    public int updateAccount(User user) throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        final String query;
        if (user instanceof KorAPUser) {
            KorAPUser k = (KorAPUser) user;
            np.addValue("ali", k.getAccountLink());
            np.addValue("alo", k.isAccountLocked());
            if (k.getPassword() != null)
                np.addValue("ps", k.getPassword());
            URIParam param = k.getField(URIParam.class);
            if (param != null) {
                np.addValue("frag", param.getUriFragment());
                np.addValue("exp", new Date(param.getUriExpiration()));
            }else {
                np.addValue("frag", null);
                np.addValue("exp", null);
            }
            np.addValue("id", k.getId());

            query = "UPDATE korap_users SET account_lock=:alo," +
                    "account_link=:ali, password=:ps," +
                    "uri_fragment=:frag," +
                    "uri_expiration=:exp WHERE id=:id";
        }else if (user instanceof ShibUser) {
            ShibUser s = (ShibUser) user;
            //todo:
            //            np.addValue("ali", s.getAccountLink());
            np.addValue("ali", null);
            np.addValue("edu", s.getAffiliation());
            np.addValue("id", s.getId());
            np.addValue("cn", s.getCn());
            np.addValue("mail", s.getMail());

            query = "UPDATE shibusers SET account_link=:ali" +
                    " eduPersonScopedAffiliation=:edu" +
                    "mail=:mail, cn=:cn WHERE id=:id";
        }else
            return -1;
        try {
            return this.jdbcTemplate.update(query, np);
        }catch (DataAccessException e) {
            jlog.error(
                    "Could not update user account for user: " + user.getId(),
                    e);
            //            throw new KorAPException(e, StatusCodes.CONNECTION_ERROR);
            throw new dbException(user.getId(), "korap_users",
                    StatusCodes.DB_UPDATE_FAILED, user.toString());
        }
    }

    @Override
    public int createAccount(User user) throws KustvaktException {
        final String query;
        MapSqlParameterSource np = new MapSqlParameterSource();

        if (user instanceof KorAPUser) {
            final KorAPUser k = (KorAPUser) user;

            URIParam param = k.getField(URIParam.class);
            np.addValue("us", k.getUsername());
            np.addValue("alo", k.isAccountLocked());
            np.addValue("ali", k.getAccountLink());
            np.addValue("ps", k.getPassword());
            if (param != null) {
                np.addValue("uri", param.getUriFragment());
                np.addValue("urie", param.getUriExpiration());
            }else {
                np.addValue("uri", null);
                np.addValue("urie", null);
            }

            np.addValue("acr", System.currentTimeMillis());
            np.addValue("id", k.getId());

            if (user.getId() != -1)
                query = "INSERT INTO korap_users (id, username, account_lock, "
                        +
                        "account_link, password, uri_fragment, " +
                        "account_creation, " +
                        "uri_expiration) VALUES (:id, :us, :alo, :ali, " +
                        ":ps, :uri, :acr, :urie);";
            else
                query = "INSERT INTO korap_users (username, account_lock, " +
                        "account_link, password, uri_fragment, " +
                        "account_creation, " +
                        "uri_expiration) VALUES (:us, :alo, :ali, " +
                        ":ps, :uri, :acr, :urie);";

            //fixme: still applicable?
        }else if (user instanceof ShibUser) {
            ShibUser s = (ShibUser) user;

            query = "INSERT INTO shibusers (username, type, account_link, account_creation "
                    +
                    "eduPersonScopedAffiliation, cn, mail) " +
                    "VALUES (:us, :type, :ali, " +
                    ":edu, :cn, :mail, :logs, :logft);";
            np.addValue("us", s.getUsername());
            //            np.addValue("ali", s.getAccountLink());
            np.addValue("ali", null);
            np.addValue("edu", s.getAffiliation());
            np.addValue("mail", s.getMail());
            np.addValue("type", user.getType());
            np.addValue("cn", s.getCn());
            np.addValue("acr", System.currentTimeMillis());

            //todo: disable after first intro
        }else if (user instanceof DemoUser) {
            query = "INSERT INTO korap_users (username, type, account_lock, account_creation "
                    +
                    "password, uri_fragment, " +
                    "uri_expiration) VALUES (:us, :type, :alo, " +
                    ":ps, :uri, :urie);";

            np.addValue("us", user.getUsername());
            np.addValue("type", user.getType());
            //            np.addValue("ali", user.getAccountLink());
            np.addValue("ali", null);
            np.addValue("alo", user.isAccountLocked());
            np.addValue("urie", new Date(0));
            np.addValue("ps", DemoUser.PASSPHRASE);
            np.addValue("uri", "");
            np.addValue("acr", System.currentTimeMillis());
        }else
            return -1;

        KeyHolder holder = new GeneratedKeyHolder();

        try {
            int r = this.jdbcTemplate
                    .update(query, np, holder, new String[] { "id" });
            user.setId(holder.getKey().intValue());

            if (user.getDetails() == null)
                user.setDetails(new UserDetails());
            if (user.getSettings() == null)
                user.setSettings(new UserSettings());

            this.createUserDetails(user.getDetails());
            this.createSettings(user.getSettings());
            return r;
        }catch (DataAccessException e) {
            jlog.error("Could not create user account with username: {}",
                    user.getUsername());
            e.printStackTrace();
            throw new dbException(user.getUsername(), "korap_users",
                    StatusCodes.NAME_EXISTS, user.getUsername());
        }
    }

    @Override
    public int deleteAccount(final Integer userid) throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("user", userid);

        try {
            int r;
            r = this.jdbcTemplate
                    .update("DELETE FROM korap_users WHERE id=:user", s);
            //            if (user instanceof KorAPUser)
            //                r = this.jdbcTemplate
            //                        .update("DELETE FROM korap_users WHERE username=:user",
            //                                s);
            //            else if (user instanceof ShibUser)
            //                r = this.jdbcTemplate
            //                        .update("DELETE FROM shibusers WHERE username=:user",
            //                                s);
            //            else
            //                r = -1;
            return r;
        }catch (DataAccessException e) {
            jlog.error("Could not delete account for user: " + userid, e);
            //            throw new KorAPException(e, StatusCodes.CONNECTION_ERROR);
            throw new dbException(userid, "korap_users",
                    StatusCodes.DB_DELETE_FAILED, userid.toString());
        }

    }

    @Override
    public int resetPassphrase(String username, String uriToken,
            String passphrase) throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        final String query = "UPDATE korap_users SET " +
                "uri_expiration=0, password=:pass WHERE uri_fragment=:uri AND uri_expiration > :now "
                + "AND username=:us AND uri_expiration > :now;";
        np.addValue("uri", uriToken);
        np.addValue("now", new Date(TimeUtils.getNow().getMillis()));
        np.addValue("pass", passphrase);
        np.addValue("us", username);
        try {
            return this.jdbcTemplate.update(query, np);
        }catch (DataAccessException e) {
            jlog.error("Could not reset password for name: " + username, e);
            throw new dbException(username, "korap_users",
                    StatusCodes.DB_UPDATE_FAILED, username, uriToken,
                    passphrase);
        }
    }

    @Override
    public int activateAccount(String username, String uriToken)
            throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        final String query = "UPDATE korap_users SET uri_fragment='', " +
                "uri_expiration=0, account_lock=:lock WHERE uri_fragment=:uri AND username=:us AND "
                +
                "uri_expiration > :now;";
        np.addValue("uri", uriToken);
        np.addValue("now", TimeUtils.getNow().getMillis());
        np.addValue("us", username);
        np.addValue("lock", BooleanUtils.getBoolean(false));
        try {
            return this.jdbcTemplate.update(query, np);
        }catch (DataAccessException e) {
            jlog.error("Could not confirm registration for name " + username,
                    e);
            throw new dbException(username, "korap_users",
                    StatusCodes.DB_UPDATE_FAILED, username, uriToken);
        }
    }

    public int size() {
        final String query = "SELECT COUNT(*) FROM korap_users;";
        return this.jdbcTemplate
                .queryForObject(query, new HashMap<String, Object>(),
                        Integer.class);

    }

    //todo:
    public List getAccountLinks(User user) {

        return Collections.emptyList();
    }

    //todo:
    public void setAccountParameters(User user) {
        ParamFields fields = user.getFields();
    }
}
