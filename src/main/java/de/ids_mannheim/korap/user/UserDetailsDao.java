package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import org.springframework.dao.DataAccessException;
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
public class UserDetailsDao implements UserDataDbIface<Userdetails2> {

    private NamedParameterJdbcTemplate jdbcTemplate;

    public UserDetailsDao(PersistenceClient client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }

    @Override
    public Class<Userdetails2> getType() {
        return Userdetails2.class;
    }

    @Override
    public int store(Userdetails2 data) {
        String sql = "INSERT INTO user_details2 (user_id, data) VALUES (:userid, :data);";
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
    public int update(Userdetails2 data) {
        String sql = "UPDATE user_details2 SET data = :data WHERE user_id=:userid;";
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
    public Userdetails2 get(Integer id) {
        String sql = "SELECT * FROM user_details2 WHERE id=:id;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("id", id);

        try {
            return this.jdbcTemplate
                    .queryForObject(sql, source, new RowMapper<Userdetails2>() {

                        @Override
                        public Userdetails2 mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            Userdetails2 details = new Userdetails2(
                                    rs.getInt("user_id"));
                            details.setId(rs.getInt("id"));
                            details.setData(rs.getString("data"));
                            return details;
                        }
                    });

        }catch (DataAccessException e) {
            return null;
        }
    }

    @Override
    public Userdetails2 get(User user) {
        String sql = "SELECT * FROM user_details2 WHERE user_id=:userid;";
        MapSqlParameterSource source = new MapSqlParameterSource();
        source.addValue("userid", user.getId());

        try {
            return this.jdbcTemplate
                    .queryForObject(sql, source, new RowMapper<Userdetails2>() {

                        @Override
                        public Userdetails2 mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            Userdetails2 details = new Userdetails2(
                                    rs.getInt("user_id"));
                            details.setId(rs.getInt("id"));
                            details.setData(rs.getString("data"));
                            return details;
                        }
                    });

        }catch (DataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int delete(Userdetails2 data) {
        String sql = "DELETE FROM user_details2 WHERE id=:id";
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
        String sql = "DELETE FROM user_details2;";
        try {
            return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
        }catch (DataAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
