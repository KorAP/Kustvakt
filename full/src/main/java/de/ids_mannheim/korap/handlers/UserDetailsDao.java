package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.DatabaseException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetails;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author hanl
 * @date 27/01/2016
 */
@Deprecated
public class UserDetailsDao implements UserDataDbIface<UserDetails> {

    private NamedParameterJdbcTemplate jdbcTemplate;


    public UserDetailsDao (PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }


    @Override
    public int store (UserDetails data) throws KustvaktException {
        String sql = "INSERT INTO user_details (user_id, data) VALUES (:userid, :data);";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", data.getUserId());
        source.addValue("data", data.serialize());

        GeneratedKeyHolder gen = new GeneratedKeyHolder();
        try {
            this.jdbcTemplate.update(sql, source, gen);
            int id = gen.getKey().intValue();
            data.setId(id);
            return id;
        }
        catch (DataAccessException e) {
            return -1;
        }
    }


    @Override
    public int update (UserDetails data) throws KustvaktException {
        String sql = "UPDATE user_details SET data = :data WHERE user_id=:userid;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", data.getUserId());
        source.addValue("data", data.serialize());

        try {
            return this.jdbcTemplate.update(sql, source);
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }


    @Override
    public UserDetails get (Integer id) throws DatabaseException {
        String sql = "SELECT * FROM user_details WHERE id=:id;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);

        try {
            return this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapper<UserDetails>() {

                        @Override
                        public UserDetails mapRow (ResultSet rs, int rowNum)
                                throws SQLException {
                            UserDetails details = new UserDetails();
                            details.setUserId(rs.getInt("user_id"));
                            details.setId(rs.getInt("id"));
                            details.setData(rs.getString("data"));
                            return details;
                        }
                    });

        }
        catch (EmptyResultDataAccessException ex) {
            return null;
        }
        catch (DataAccessException e) {
            throw new DatabaseException(-1, "userDetails",
                    StatusCodes.REQUEST_INVALID, "The request is invalid.",
                    String.valueOf(id));
        }
    }


    @Override
    public UserDetails get (User user) throws DatabaseException {
        String sql = "SELECT * FROM user_details WHERE user_id=:userid;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", user.getId());

        try {
            return this.jdbcTemplate.queryForObject(sql, source,
                    new RowMapper<UserDetails>() {

                        @Override
                        public UserDetails mapRow (ResultSet rs, int rowNum)
                                throws SQLException {
                            UserDetails details = new UserDetails();
                            details.setUserId(rs.getInt("user_id"));
                            details.setId(rs.getInt("id"));
                            details.setData(rs.getString("data"));
                            return details;
                        }
                    });
        }
        catch (EmptyResultDataAccessException ex) {
            return null;
        }
        catch (DataAccessException e) {
            throw new DatabaseException(user.getId(), "userDetails",
                    StatusCodes.REQUEST_INVALID, "The request is invalid.");
        }
    }


    @Override
    public int delete (UserDetails data) {
        String sql = "DELETE FROM user_details WHERE id=:id";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", data.getId());
        try {
            return this.jdbcTemplate.update(sql, source);
        }
        catch (DataAccessException e) {
            return -1;
        }
    }


    @Override
    public int deleteAll () {
        String sql = "DELETE FROM user_details;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }
        catch (DataAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }


    @Override
    public Class<UserDetails> type () {
        return UserDetails.class;
    }
}
