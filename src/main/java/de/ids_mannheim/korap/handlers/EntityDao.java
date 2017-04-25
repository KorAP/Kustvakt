package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.KustvaktBaseDaoInterface;
import de.ids_mannheim.korap.config.ParamFields;
import de.ids_mannheim.korap.config.URIParam;
import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.DatabaseException;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.ShibUser;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.BooleanUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* WKP: In computer software, a data access object (DAO) is an object that provides an abstract interface to some type 
 * of database or other persistence mechanism. By mapping application calls to the persistence layer, the DAO provides 
 * some specific data operations without exposing details of the database. 
 */

/**
 * @author hanl
 * @date 13/01/2014
 */
public class EntityDao implements EntityHandlerIface, KustvaktBaseDaoInterface {

    private static Logger jlog = LoggerFactory.getLogger(EntityDao.class);
    private NamedParameterJdbcTemplate jdbcTemplate;


    public EntityDao (PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }


    // usersettings are fetched plus basic account info, no details, since i rarely use them anyway!
    @Override
    public User getAccount (String username) throws KustvaktException {
        Map<String, String> namedParameters = Collections
                .singletonMap("username", username);
        final String sql = "select a.* from korap_users as a where a.username=:username;";
        User user;
        try {
            user = this.jdbcTemplate.queryForObject(sql, namedParameters,
                    new RowMapperFactory.UserMapper());
        }
        catch (EmptyResultDataAccessException ae) {
            jlog.warn("No user found for name '{}'", username);
            throw new EmptyResultException(username);
        }
        catch (DataAccessException e) {
            jlog.error("Could not retrieve user for name: " + username, e);
            throw new DatabaseException(username, "korap_users",
                    StatusCodes.DB_GET_FAILED,
                    "Could not retrieve the user with username: " + username,
                    username);
        }
        return user;
    }


    @Override
    public int updateAccount (User user) throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        final String query;
        if (user instanceof KorAPUser) {
            KorAPUser k = (KorAPUser) user;
            np.addValue("ali", k.getAccountLink());
            np.addValue("alo", k.isAccountLocked());
            if (k.getPassword() != null)
                np.addValue("ps", k.getPassword());
            //            URIParam param = k.getField(URIParam.class);
            //            if (param != null) {
            //                np.addValue("frag", param.getUriFragment());
            //                np.addValue("exp", new Date(param.getUriExpiration()));
            //            }else {
            //                np.addValue("frag", null);
            //                np.addValue("exp", null);
            //            }
            np.addValue("id", k.getId());

            query = "UPDATE korap_users SET account_lock=:alo,"
                    + "account_link=:ali, password=:ps "
                    //                    "uri_fragment=:frag," +
                    //                    "uri_expiration=:exp "
                    + "WHERE id=:id";
        }
        else if (user instanceof ShibUser) {
            ShibUser s = (ShibUser) user;
            //todo:
            //            np.addValue("ali", s.getAccountLink());
            np.addValue("ali", null);
            np.addValue("edu", s.getAffiliation());
            np.addValue("id", s.getId());
            np.addValue("cn", s.getCn());
            np.addValue("mail", s.getMail());

            query = "UPDATE shibusers SET account_link=:ali"
                    + " eduPersonScopedAffiliation=:edu"
                    + "mail=:mail, cn=:cn WHERE id=:id";
        }
        else
            return -1;
        try {
            return this.jdbcTemplate.update(query, np);
        }
        catch (DataAccessException e) {
            jlog.error(
                    "Could not update user account for user: " + user.getId(),
                    e);
            throw new DatabaseException(user.getId(), "korap_users",
                    StatusCodes.DB_UPDATE_FAILED,
                    "Could not update user account for user: " + user.getId(),
                    user.toString());
        }
    }


    @Override
    public int createAccount (User user) throws KustvaktException {
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
            }
            else {
                np.addValue("uri", null);
                np.addValue("urie", null);
            }

            np.addValue("acr", System.currentTimeMillis());
            np.addValue("id", k.getId());

            if (user.getId() != -1)
                query = "INSERT INTO korap_users (id, username, account_lock, "
                        + "account_link, password, uri_fragment, "
                        + "account_creation, "
                        + "uri_expiration) VALUES (:id, :us, :alo, :ali, "
                        + ":ps, :uri, :acr, :urie);";
            else
                query = "INSERT INTO korap_users (username, account_lock, "
                        + "account_link, password, uri_fragment, "
                        + "account_creation, "
                        + "uri_expiration) VALUES (:us, :alo, :ali, "
                        + ":ps, :uri, :acr, :urie);";

            //fixme: still applicable?
        }
        else if (user instanceof ShibUser) {
            ShibUser s = (ShibUser) user;

            query = "INSERT INTO shibusers (username, type, account_link, account_creation "
                    + "eduPersonScopedAffiliation, cn, mail) "
                    + "VALUES (:us, :type, :ali, "
                    + ":edu, :cn, :mail, :logs, :logft);";
            np.addValue("us", s.getUsername());
            np.addValue("ali", null);
            np.addValue("edu", s.getAffiliation());
            np.addValue("mail", s.getMail());
            np.addValue("type", user.getType());
            np.addValue("cn", s.getCn());
            np.addValue("acr", System.currentTimeMillis());

            //todo: deprecate
            //        }else if (user instanceof DemoUser) {
            //            query = "INSERT INTO korap_users (id, username, type, account_lock, account_creation) VALUES "
            //                    + "(:id, :us, :type, :alo);";
            //
            //            np.addValue("id", user.getId());
            //            np.addValue("us", user.getUsername());
            //            np.addValue("type", user.getType());
            //            np.addValue("alo", user.isAccountLocked());
            //            np.addValue("acr", System.currentTimeMillis());
        }
        else
            return -1;

        KeyHolder holder = new GeneratedKeyHolder();

        int r;
        try {
            r = this.jdbcTemplate.update(query, np, holder,
                    new String[] { "id" });
            user.setId(holder.getKey().intValue());
        }
        catch (DuplicateKeyException e) {
            jlog.error("Could not create user account with username: {}",
                    user.getUsername());
            throw new DatabaseException(user.getUsername(), "korap_users",
                    StatusCodes.ENTRY_EXISTS, "Username exists.",
                    user.getUsername());
        }
        catch (DataAccessException e) {
            throw new DatabaseException(user.getUsername(), "korap_users",
                    StatusCodes.ENTRY_EXISTS, "Username exists.",
                    user.getUsername());
        }

        return r;
    }


    @Override
    public int deleteAccount (final Integer userid) throws KustvaktException {
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
        }
        catch (DataAccessException e) {
            jlog.error("Could not delete account for user: " + userid, e);
            //            throw new KorAPException(e, StatusCodes.CONNECTION_ERROR);
            throw new DatabaseException(userid, "korap_users",
                    StatusCodes.DB_DELETE_FAILED,
                    "Could not delete account for user: " + userid,
                    userid.toString());
        }

    }


    @Override
    public int truncate () {
        String sql = "DELETE FROM korap_users;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }
        catch (DataAccessException e) {
            return -1;
        }
    }


    @Override
    public int resetPassphrase (String username, String uriToken,
            String passphrase) throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        final String query = "UPDATE korap_users SET "
                + "uri_expiration=0, password=:pass WHERE uri_fragment=:uri AND uri_expiration > :now "
                + "AND username=:us AND uri_expiration > :now;";
        np.addValue("uri", uriToken);
        np.addValue("now", new Date(TimeUtils.getNow().getMillis()));
        np.addValue("pass", passphrase);
        np.addValue("us", username);
        try {
            return this.jdbcTemplate.update(query, np);
        }
        catch (DataAccessException e) {
            jlog.error("Could not reset password for name: " + username, e);
            throw new DatabaseException(username, "korap_users",
                    StatusCodes.DB_UPDATE_FAILED,
                    "Could not reset password for username: " + username,
                    username, uriToken, passphrase);
        }
    }


    @Override
    public int activateAccount (String username, String uriToken)
            throws KustvaktException {
        MapSqlParameterSource np = new MapSqlParameterSource();
        final String query = "UPDATE korap_users SET uri_fragment='', "
                + "uri_expiration=0, account_lock=:lock WHERE uri_fragment=:uri AND username=:us AND "
                + "uri_expiration > :now;";
        np.addValue("uri", uriToken);
        np.addValue("now", TimeUtils.getNow().getMillis());
        np.addValue("us", username);
        np.addValue("lock", BooleanUtils.getBoolean(false));
        try {
            return this.jdbcTemplate.update(query, np);
        }
        catch (DataAccessException e) {
            jlog.error("Could not confirm registration for name " + username,
                    e);
            throw new DatabaseException(username, "korap_users",
                    StatusCodes.DB_UPDATE_FAILED,
                    "Could not confirm registration for username " + username,
                    username, uriToken);
        }
    }


    @Override
    public int size () {
        final String query = "SELECT COUNT(*) FROM korap_users;";
        return this.jdbcTemplate.queryForObject(query,
                new HashMap<String, Object>(), Integer.class);
    }


    //todo:
    public List getAccountLinks (User user) {

        return Collections.emptyList();
    }


    //todo:
    public void setAccountParameters (User user) {
        ParamFields fields = user.getFields();
    }
}
