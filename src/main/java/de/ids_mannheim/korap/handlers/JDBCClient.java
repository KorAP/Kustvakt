package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.utils.BooleanUtils;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author hanl
 * @date 13/01/2014
 */
@Data
public class JDBCClient extends PersistenceClient<NamedParameterJdbcTemplate> {

    @Setter(AccessLevel.NONE)
    private DataSource dataSource;

    public JDBCClient(DataSource datasource) {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(
                datasource);
        template.setCacheLimit(500);
        super.setSource(template);
        this.dataSource = datasource;
    }

    public JDBCClient(DataSource dataSource, Resource resource)
            throws IOException {
        this(dataSource);
        this.setSchema(resource.getInputStream());
    }

    @Override
    public void setSchema(InputStream stream) throws IOException {
        super.setSchema(stream);
    }

    @Override
    public boolean checkDatabase() {
        int size;
        NamedParameterJdbcTemplate tmp = this.getSource();
        try {
            // uses flyway schema table to determine of schema was applied succesfully
            size = tmp.queryForObject(
                    "select count(*) from schema_version limit 10;",
                    new HashMap<String, Object>(), Integer.class);
        }catch (Exception e) {
            System.out.println("No database schema found!");
            return false;
        }
        return size > 0;
    }

    @Override
    public void setDatabase(String name) {
        super.setDatabase(name);
        BooleanUtils.dbname = name;
    }

    // get schema file from configuration and create database
    @Override
    public void createDatabase() {
        if (!checkDatabase()) {
            final ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();
            rdp.addScript(new InputStreamResource(this.getSchema()));
            rdp.setSeparator("$$");
            try {
                rdp.populate(this.dataSource.getConnection());
            }catch (SQLException e) {
                // do nothing
                e.printStackTrace();
            }
        }
    }

}
