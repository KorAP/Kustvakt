package de.ids_mannheim.korap.handlers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import de.ids_mannheim.korap.config.KustvaktBaseDaoInterface;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.AdminHandlerIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.User;

public class AdminDao implements AdminHandlerIface, KustvaktBaseDaoInterface {
	
	private static Logger jlog = LoggerFactory.getLogger(AdminDao.class);
	private NamedParameterJdbcTemplate jdbcTemplate;

	public AdminDao(PersistenceClient client) {
		this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
	}
	
	@Override
	public int addAccount(User user) throws KustvaktException{
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("user_id", user.getId());
		String query = "INSERT INTO admin_users (user_id) VALUES (:user_id)";
		try {
            int r = this.jdbcTemplate.update(query, params);
            return r;
        }
        catch (DataAccessException e) {
            jlog.warn("Could not add {} as an admin. {} is already an admin.",
                    user.getUsername());
//            throw new dbException(user.getId().toString(), "admin_users",
//                    StatusCodes.ENTRY_EXISTS, user.getId().toString());
            return 0;
        }
	}

	@Override
	public int size() {
		final String query = "SELECT COUNT(*) FROM admin_users;";
		return this.jdbcTemplate.queryForObject(query, new HashMap<String, Object>(), Integer.class);
	}

	@Override
	public int truncate() {
		String sql = "DELETE FROM korap_users;";
		try {
			return this.jdbcTemplate.update(sql, new HashMap<String, Object>());
		} catch (DataAccessException e) {
			return -1;
		}
	}

	@Override
	public int updateAccount(User user) throws KustvaktException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int deleteAccount(Integer userid) throws KustvaktException {
		// TODO Auto-generated method stub
		return 0;
	}

	// EM: FIX ME
	@Override
	public boolean isAdmin(int userId) {
		Map<String, String> namedParameters = Collections.singletonMap(
                "user_id", String.valueOf(userId));
		
        final String sql = "select id from admin_users where user_id=:user_id;";
        try {
            List<Map<String, Object>> ids = this.jdbcTemplate.queryForList(sql, namedParameters);
            if (ids.isEmpty()){
            	return false;
            }
        }
        catch (DataAccessException e) {
            return false;
        }
		return true;
	}

}
