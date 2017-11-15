package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.ClientInfo;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.DatabaseException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.AuthenticationType;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.BooleanUtils;
import de.ids_mannheim.korap.utils.TimeUtils;
import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Created by hanl on 7/14/14.
 */
public class OAuthDb {

    private static final Logger jlog = LoggerFactory.getLogger(OAuthDb.class);
    private NamedParameterJdbcTemplate jdbcTemplate;


    public OAuthDb (PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }


    public ClientInfo getClient (String clientid) {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("cl", clientid);
        String sql = "select * from oauth2_client where client_id=:cl;";

        try {
            return this.jdbcTemplate.queryForObject(sql, s,
                    new RowMapper<ClientInfo>() {
                        @Override
                        public ClientInfo mapRow (ResultSet rs, int rowNum)
                                throws SQLException {
                            ClientInfo info = new ClientInfo(rs
                                    .getString("client_id"), rs
                                    .getString("client_secret"));
                            info.setId(rs.getInt("id"));
                            info.setClient_type(rs.getString("client_type"));
                            info.setRedirect_uri(rs.getString("redirect_uri"));
                            info.setUrl(rs.getString("url"));
                            info.setConfidential(rs
                                    .getBoolean("is_confidential"));
                            return info;
                        }
                    });
        }
        catch (EmptyResultDataAccessException ex) {
            jlog.error("'{}' client found", clientid, ex.fillInStackTrace());
            return null;
        }
    }


    // fixme: what to delete? difference client/application table?
    public boolean revokeToken (String token) throws KustvaktException {
        String sql = "delete from oauth2_access_token WHERE access_token=:token;";
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("token", token);
        try {
            return this.jdbcTemplate.update(sql, s) == 1;
        }
        catch (DataAccessException e) {
            jlog.error("token could not be revoked", e.fillInStackTrace());
            return false;
        }
    }


    public boolean revokeAuthorization (ClientInfo info, User user) {
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("us", user.getId());
        source.addValue("cls", info.getClient_secret());
        source.addValue("clid", info.getClient_id());

        String tokens = "delete from oauth2_access_token where user_id=:us and client_id in "
                + "(select client_id from oauth2_client where client_id=:clid and client_secret=:cls);";

        try {
            this.jdbcTemplate.update(tokens, source);
        }
        catch (DataAccessException e) {
            jlog.error("authorization could not be revoked for user '{}'",
                    user.getUsername());
            return false;
        }
        //fixme: if int row not updated, false!!
        return true;
    }


    public boolean addToken (String token, String refresh, Integer userid,
            String client_id, String scopes, int expiration)
            throws KustvaktException {
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("token", token);
        s.addValue("rt", refresh);
        s.addValue("ex", new Timestamp(TimeUtils.plusSeconds(expiration)
                .getMillis()));
        s.addValue("us", userid);
        s.addValue("sc", scopes);
        s.addValue("st", BooleanUtils.getBoolean(true));
        s.addValue("cli", client_id);
        String sql = "insert into oauth2_access_token (access_token, refresh_token, scopes, client_id, user_id, expiration, status) "
                + "values (:token, :rt, :sc, :cli, :us, :ex, :st);";
        try {
            return this.jdbcTemplate.update(sql, s) == 1;
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            jlog.error("token '{}' could not be added for user '{}'", token,
                    userid);
            return false;
        }
    }


    // returns the first token to find
    public String getToken (String client_id, Integer userid) {
        String sql = "select access_token from oauth2_access_token where user_id=:uid"
                + " and status=1 and client_id=:cli limit 1;";
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("uid", userid);
        s.addValue("cli", client_id);
        try {
            return this.jdbcTemplate.queryForObject(sql, s, String.class);
        }
        catch (EmptyResultDataAccessException ex) {
            jlog.error("no token found for user '{}'", userid);
            return null;
        }
        catch (DataAccessException ex) {
            jlog.error("token retrieval failed for user '{}'", userid);
            return null;
        }
    }


    public List<ClientInfo> getAuthorizedClients (Integer userid) {
        String sql = "select cl.* from oauth2_client as cl where cl.client_id in (select cd.client_id from oauth2_access_token as cd "
                + "where cd.user_id=:user) or cl.is_confidential=:conf;";

        //todo: test query
        //        "select cl.* from oauth2_client as cl inner join oauth2_access_token as cd "
        //                + "on cd.client_id=cl.client_id where cd.user_id=:user or cl.is_confidential=:conf;"

        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("user", userid);
        s.addValue("conf", BooleanUtils.getBoolean(true));
        try {
            // secret is not returned for this function
            return this.jdbcTemplate.query(sql, s, new RowMapper<ClientInfo>() {

                @Override
                public ClientInfo mapRow (ResultSet rs, int rowNum)
                        throws SQLException {
                    ClientInfo info = new ClientInfo(rs.getString("client_id"), "*****");
                    info.setConfidential(rs.getBoolean("is_confidential"));
                    info.setUrl(rs.getString("url"));
                    info.setId(rs.getInt("id"));
                    info.setRedirect_uri(rs.getString("redirect_uri"));
                    return info;
                }
            });
        }
        catch (DataAccessException e) {
            jlog.error("Data access error", e);
            return Collections.emptyList();
        }

    }


    // todo: expired token must trigger an invalid token exception to trigger a refresh token
    public TokenContext getContext (final String token)
            throws KustvaktException {
        String sql = "select ko.username, oa.expiration, oa.scopes from oauth2_access_token as oa inner join korap_users as ko "
                + "on ko.id=oa.user_id where oa.access_token=:token and oa.expiration > :now;";
        MapSqlParameterSource s = new MapSqlParameterSource();
        s.addValue("token", token);
        s.addValue("now", new Timestamp(TimeUtils.getNow().getMillis()));

        try {
            TokenContext context = this.jdbcTemplate.queryForObject(sql, s,
                    new RowMapper<TokenContext>() {
                        @Override
                        public TokenContext mapRow (ResultSet rs, int rowNum)
                                throws SQLException {
                            long exp = rs.getTimestamp("expiration").getTime();
                            TokenContext c = new TokenContext();
                            c.setUsername(rs.getString(Attributes.USERNAME));
                            c.setExpirationTime(exp);
                            c.setToken(token);
                            c.setAuthenticationType(AuthenticationType.OAUTH2);
                            //.setTokenType(Attributes.OAUTH2_AUTHORIZATION);
                            c.addContextParameter(Attributes.SCOPES,
                                    rs.getString(Attributes.SCOPES));
                            return c;
                        }
                    });
            return context;
        }
        catch (EmptyResultDataAccessException ee) {
            jlog.error("no context found for token '{}'", token);
            revokeToken(token);
            throw new KustvaktException(StatusCodes.EXPIRED, "token", token);
        }
        catch (DataAccessException e) {
            jlog.error("token context retrieval failed for '{}'", token);
            throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT,
                    "invalid token", token);
        }

    }


    // subsequently delete all access and auth code tokens associated!
    public void removeClient (ClientInfo info, User user)
            throws KustvaktException {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("url", info.getUrl());
        p.addValue("cls", info.getClient_secret());
        p.addValue("clid", info.getClient_id());
        String sql = "delete from oauth2_client where client_id=:clid and client_secret=:cls and"
                + " url=:url;";
        try {
            this.jdbcTemplate.update(sql, p);
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            jlog.error("removing client '{}' failed", info.getClient_id());
            throw new DatabaseException(new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "arguments given not valid",
                    info.toJSON()), StatusCodes.CLIENT_REMOVAL_FAILURE,
                    info.toJSON());

        }
    }


    public void registerClient (ClientInfo info, User user)
            throws KustvaktException {
        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("clid", info.getClient_id());
        p.addValue("con", info.isConfidential());
        p.addValue("cls", info.getClient_secret());
        p.addValue("clt", info.getClient_type());
        p.addValue("url", info.getUrl());
        p.addValue("r_url", info.getRedirect_uri());
        String sql = "insert into oauth2_client (client_id, client_secret, client_type, url, is_confidential, redirect_uri) "
                + "VALUES (:clid, :cls, :clt, :url, :con, :r_url);";
        try {
            this.jdbcTemplate.update(sql, p);
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            jlog.error("registering client '{}' failed", info.getClient_id());
            throw new DatabaseException(new KustvaktException(user.getId(),
                    StatusCodes.ILLEGAL_ARGUMENT, "arguments given not valid",
                    info.toJSON()), StatusCodes.CLIENT_REGISTRATION_FAILED,
                    info.toJSON());
        }
    }
}
