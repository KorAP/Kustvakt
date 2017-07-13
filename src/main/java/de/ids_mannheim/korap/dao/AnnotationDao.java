package de.ids_mannheim.korap.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


/**
 * AnnotationDao manages SQL queries regarding information about
 * annotations, e.g foundries and layers.
 * 
 * @author margaretha
 *
 */
public class AnnotationDao {

    private static Logger jlog = LoggerFactory.getLogger(AnnotationDao.class);
    private NamedParameterJdbcTemplate jdbcTemplate;


    public AnnotationDao () {
        // TODO Auto-generated constructor stub
    }
    
    
}
