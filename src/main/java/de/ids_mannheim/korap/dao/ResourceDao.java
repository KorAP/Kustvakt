package de.ids_mannheim.korap.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import de.ids_mannheim.korap.handlers.RowMapperFactory;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;

/** ResourceDao manages SQL queries regarding resource info and layers.
 * 
 * @author margaretha
 *
 */
public class ResourceDao {

    private static Logger jlog = LoggerFactory.getLogger(ResourceDao.class);
    private NamedParameterJdbcTemplate jdbcTemplate;


    public ResourceDao (PersistenceClient<?> client) {
        this.jdbcTemplate = (NamedParameterJdbcTemplate) client.getSource();
    }

    // EM: use JPA?
    public void getResourceInfo (String resourceId) {
        SqlParameterSource namedParameters = new MapSqlParameterSource(
                "resourceId", resourceId);
        String sql = "select * from resource where id=:resourceId";
//        this.jdbcTemplate.queryForObject(sql, namedParameters,
//                new RowMapperFactory.ResourceMapper());
    }

}
