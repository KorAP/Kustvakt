package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import de.ids_mannheim.korap.authentication.KustvaktAuthenticationManager;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.JDBCClient;
import de.ids_mannheim.korap.interfaces.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.KustvaktBaseDaoInterface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.TimeUtils;

/**
 * creates a test user that can be used to access protected functions
 * 
 * @author hanl
 * @date 16/10/2015
 */
public class TestHelper {

    private static Logger jlog = LoggerFactory.getLogger(TestHelper.class);
    private static final Map<String, Object> data = new HashMap<>();
    static  {
        data.put(Attributes.ID, 3); // 2);
        data.put(Attributes.USERNAME, "testUser1"); // bodmer funktioniert noch nicht
        data.put(Attributes.PASSWORD, "testPass2015");
        data.put(Attributes.FIRSTNAME, "test");
        data.put(Attributes.LASTNAME, "user");
        data.put(Attributes.EMAIL, "test@ids-mannheim.de");
        data.put(Attributes.ADDRESS, "Mannheim");
        data.put(Attributes.DEFAULT_LEMMA_FOUNDRY, "test_l");
        data.put(Attributes.DEFAULT_POS_FOUNDRY, "test_p");
        data.put(Attributes.DEFAULT_CONST_FOUNDRY, "test_const");
    }

    private ContextHolder beansHolder;

    public static TestHelper newInstance (ApplicationContext ctx)
            throws Exception {
        TestHelper b = new TestHelper();
        b.beansHolder = new ContextHolder(ctx) {};
        return b;
    }


    public <T> T getBean (Class<T> type) {
        return this.beansHolder.getBean(type);
    }


    public ContextHolder getContext () {
        return this.beansHolder;
    }


    public <T> T getBean (String name) {
        return (T) this.beansHolder.getBean(name);
    }


    public TestHelper setupAccount () throws KustvaktException {
        KustvaktBaseDaoInterface dao = getBean(ContextHolder.KUSTVAKT_USERDB);

        KustvaktAuthenticationManager manager = getBean(ContextHolder.KUSTVAKT_AUTHENTICATION_MANAGER);
//        manager.createUserAccount(KustvaktConfiguration.KUSTVAKT_USER, false);
        try {
            User user = getUser();
            jlog.debug("found user, skipping setup ...");
            if (!user.getUsername().equals(data.get(Attributes.USERNAME))){
            	return this;
            }
        }
        catch (RuntimeException e) {
            // do nothing and continue
        }

        Map m = getUserCredentials();
        assertNotNull("userdatabase handler must not be null", dao);

        try {
            manager.createUserAccount(m, false);
        }
        catch (KustvaktException e) {
            throw new RuntimeException(e);
            /*// do nothing
            jlog.error("Error: {}", e.string());
            assertNotNull("Test user could not be set up", null);*/
        }
        assertNotEquals(0, dao.size());
        return this;
    }

    // EM
    @Deprecated
//    public TestHelper setupSimpleAccount (String username, String password) {
//        KustvaktBaseDaoInterface dao = getBean(ContextHolder.KUSTVAKT_USERDB);
//        EntityHandlerIface edao = (EntityHandlerIface) dao;
//        try {
//            edao.getAccount(username);
//        }
//        catch (EmptyResultException e) {
//            // do nothing
//        }
//        catch (KustvaktException ex) {
//            assertNull("Test user could not be set up", true);
//        }
//
//        Map m = new HashMap<>();
//        m.put(Attributes.USERNAME, username);
//
//        try {
//            String hash = ((EncryptionIface) getBean(ContextHolder.KUSTVAKT_ENCRYPTION))
//                    .secureHash(password);
//            m.put(Attributes.PASSWORD, hash);
//        }
//        catch (NoSuchAlgorithmException | UnsupportedEncodingException
//                | KustvaktException e) {
//            // do nohting
//            assertNotNull("Exception thrown", null);
//        }
//        assertNotNull("userdatabase handler must not be null", dao);
//
//        try {
//
//            int i = edao.createAccount(User.UserFactory.toKorAPUser(m));
//            assert BeansFactory.getKustvaktContext().getUserDBHandler()
//                    .getAccount((String) data.get(Attributes.USERNAME)) != null;
//            assertEquals(1, i);
//        }
//        catch (KustvaktException e) {
//            // do nothing
//            assertNull("Test user could not be set up", true);
//        }
//        return this;
//    }


    public User getUser () {
        try {
            return ((EntityHandlerIface) getBean(ContextHolder.KUSTVAKT_USERDB))
                    .getAccount((String) data.get(Attributes.USERNAME));
        }
        catch (KustvaktException e) {
            // do nothing
        }
        throw new RuntimeException("User could not be retrieved!");
    }


    public TestHelper dropUser (String ... usernames) throws KustvaktException {
        if (usernames == null || usernames.length == 0) {
            KustvaktBaseDaoInterface dao = getBean(ContextHolder.KUSTVAKT_USERDB);
            dao.truncate();
        }
        for (String name : Arrays.asList(usernames)) {
            if (remove(name))
                break;
        }
        return this;
    }


    private boolean remove (String username) throws KustvaktException {
        EntityHandlerIface dao = getBean(ContextHolder.KUSTVAKT_USERDB);
        User us = dao.getAccount(username);
        dao.deleteAccount(us.getId());
        return true;
    }


    public TestHelper truncateAll () {
        String sql = "SELECT Concat('TRUNCATE TABLE ', TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES";
        final Set<String> queries = new HashSet<>();
        PersistenceClient cl = getBean(ContextHolder.KUSTVAKT_POLICIES);
        NamedParameterJdbcTemplate source = (NamedParameterJdbcTemplate) cl
                .getSource();

        source.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow (ResultSet rs) throws SQLException {
                queries.add(rs.getString(1));

            }
        });
//        System.out.println(queries);
        for (String query : queries)
            source.update(query, new HashMap<String, Object>());
        return this;
    }


    public static Map<String, Object> getUserCredentials () {
        return new HashMap<>(data);
    }

    public void close () {
        BeansFactory.closeApplication();
    }


    private TestHelper () {

    }

    // EM: use Spring XML config
    @Deprecated
    private static PersistenceClient mysql_db () throws IOException {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/kustvakt_test");
        dataSource.setUsername("mhanl");
        dataSource.setPassword("password");
        JDBCClient client = new JDBCClient(dataSource);
        client.setDatabase("mariadb");

        Flyway fl = new Flyway();
        fl.setDataSource(dataSource);
        fl.setLocations("db.mysql");
        fl.migrate();

        return client;
    }

    // EM: use Spring XML config
    @Deprecated
    protected static PersistenceClient sqlite_db (boolean memory)
            throws InterruptedException {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        DateTime t = TimeUtils.getNow();
        //String name = testclass != null ? testclass.getSimpleName() + "_" : "";

        if (memory)
            dataSource.setUrl("jdbc:sqlite::memory:");
        else {
            File tmp = new File("tmp");
            if (!tmp.exists())
                tmp.mkdirs();
            dataSource.setUrl("jdbc:sqlite:tmp/sqlite_" + t.getMillis()
                    + ".sqlite");
        }
        dataSource.setSuppressClose(true);

        Flyway fl = new Flyway();
        fl.setDataSource(dataSource);
        fl.setLocations("db.sqlite");
        fl.migrate();

        JDBCClient client = new JDBCClient(dataSource);
        client.setDatabase("sqlite");
        return client;
    }

    // EM: use Spring XML config
    @Deprecated
    public static PersistenceClient sqlite_db_norm (boolean memory) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setMaxTotal(1);
        dataSource.setInitialSize(1);
        dataSource.setMaxIdle(1);
        dataSource.addConnectionProperty("lazy-init", "true");
        DateTime t = TimeUtils.getNow();
        if (memory)
            dataSource.setUrl("jdbc:sqlite::memory:");
        else {
            File tmp = new File("tmp");
            if (!tmp.exists())
                tmp.mkdirs();
            dataSource.setUrl("jdbc:sqlite:tmp/sqlite_" + t.toString());
        }

        Flyway fl = new Flyway();
        fl.setDataSource(dataSource);
        fl.setLocations("db.sqlite");
        fl.migrate();

        JDBCClient client = new JDBCClient(dataSource);
        client.setDatabase("sqlite");
        return client;
    }

    // EM: use Spring XML config
    @Deprecated
    public static PersistenceClient h2_emb () throws SQLException {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:mem:");
        dataSource.getConnection().nativeSQL("SET MODE MySQL;");
        dataSource.getConnection().commit();
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDriverClassName("org.h2.Driver");

        Flyway fl = new Flyway();
        fl.setDataSource(dataSource);
        fl.setLocations("db.mysql");
        fl.migrate();
        JDBCClient client = new JDBCClient(dataSource);
        client.setDatabase("h2");
        return client;
    }

    }
