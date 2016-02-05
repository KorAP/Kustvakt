package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.exceptions.dbException;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserSettings;
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
 * @date 28/01/2016
 */
public class UserSettingsDao implements UserDataDbIface<UserSettings> {

    NamedParameterJdbcTemplate jdbcTemplate;

    public UserSettingsDao(PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }

    @Override
    public int store(UserSettings data) {
        String sql = "INSERT INTO user_settings (user_id, data) VALUES (:userid, :data);";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", data.getUserID());
        source.addValue("data", data.data());

        GeneratedKeyHolder gen = new GeneratedKeyHolder();
        try {
            this.jdbcTemplate.update(sql, source, gen);
            int id = gen.getKey().intValue();
            data.setId(id);
            return id;
        }catch (DataAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int update(UserSettings data) {
        String sql = "UPDATE user_settings SET data = :data WHERE user_id=:userid;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", data.getUserID());
        source.addValue("data", data.data());

        try {
            return this.jdbcTemplate.update(sql, source);
        }catch (DataAccessException e) {
            return -1;
        }
    }

    @Override
    public UserSettings get(Integer id) throws dbException {
        String sql = "SELECT * FROM user_settings WHERE id=:id;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);

        try {
            return this.jdbcTemplate
                    .queryForObject(sql, source, new RowMapper<UserSettings>() {

                        @Override
                        public UserSettings mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            UserSettings details = new UserSettings(
                                    rs.getInt("user_id"));
                            details.setId(rs.getInt("id"));
                            details.setData(rs.getString("data"));
                            return details;
                        }
                    });

        }catch (EmptyResultDataAccessException ex) {
            return null;
        }catch (DataAccessException e) {
            throw new dbException(-1, "userSettings",
                    StatusCodes.REQUEST_INVALID, String.valueOf(id));
        }
    }

    @Override
    public UserSettings get(User user) throws dbException {
        String sql = "SELECT * FROM user_settings WHERE user_id=:userid;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", user.getId());

        try {
            return this.jdbcTemplate
                    .queryForObject(sql, source, new RowMapper<UserSettings>() {

                        @Override
                        public UserSettings mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            UserSettings details = new UserSettings(
                                    rs.getInt("user_id"));
                            details.setId(rs.getInt("id"));
                            details.setData(rs.getString("data"));
                            return details;
                        }
                    });

        }catch (EmptyResultDataAccessException ex) {
            return null;
        }catch (DataAccessException e) {
            throw new dbException(-1, "userSettings",
                    StatusCodes.REQUEST_INVALID);
        }
    }

    @Override
    public int delete(UserSettings data) {
        String sql = "DELETE FROM user_settings WHERE id=:id";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", data.getId());
        try {
            return this.jdbcTemplate.update(sql, source);
        }catch (DataAccessException e) {
            return -1;
        }
    }

    @Override
    public int deleteAll() {
        String sql = "DELETE FROM user_settings;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }catch (DataAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
