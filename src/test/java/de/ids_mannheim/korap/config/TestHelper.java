package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.*;
import de.ids_mannheim.korap.interfaces.AuthenticationIface;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.interfaces.db.*;
import de.ids_mannheim.korap.interfaces.defaults.KustvaktEncryption;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.ac.PolicyDao;
import de.ids_mannheim.korap.security.auth.APIAuthentication;
import de.ids_mannheim.korap.security.auth.BasicHttpAuth;
import de.ids_mannheim.korap.security.auth.KustvaktAuthenticationManager;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.web.service.BootableBeanInterface;
import org.apache.commons.dbcp2.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * creates a test user that can be used to access protected functions
 * 
 * @author hanl
 * @date 16/10/2015
 */
public class TestHelper {

    private static Logger jlog = LoggerFactory.getLogger(TestHelper.class);
    private static final String[] credentials = new String[] { "test1",
            "testPass2015" };

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


    public TestHelper setupAccount () {
        KustvaktBaseDaoInterface dao = getBean(ContextHolder.KUSTVAKT_USERDB);

        KustvaktAuthenticationManager manager = getBean(ContextHolder.KUSTVAKT_AUTHENTICATION_MANAGER);

        try {
            getUser();
            jlog.debug("found user, skipping setup ...");
            return this;
        }
        catch (RuntimeException e) {
            // do nothing and continue
        }

        Map m = new HashMap<>();
        m.put(Attributes.ID, 2);
        m.put(Attributes.USERNAME, credentials[0]);
        m.put(Attributes.PASSWORD, credentials[1]);
        m.put(Attributes.FIRSTNAME, "test");
        m.put(Attributes.LASTNAME, "user");
        m.put(Attributes.EMAIL, "test@ids-mannheim.de");
        m.put(Attributes.ADDRESS, "Mannheim");
        m.put(Attributes.DEFAULT_LEMMA_FOUNDRY, "test_l");
        m.put(Attributes.DEFAULT_POS_FOUNDRY, "test_p");
        m.put(Attributes.DEFAULT_CONST_FOUNDRY, "test_const");

        assertNotNull("userdatabase handler must not be null", dao);

        try {
            manager.createUserAccount(m, false);
        }
        catch (KustvaktException e) {
            // do nothing
            assertNotNull("Test user could not be set up", null);
        }
        assertNotEquals(0, dao.size());
        return this;
    }


    public TestHelper setupSimpleAccount (String username, String password) {
        KustvaktBaseDaoInterface dao = getBean(ContextHolder.KUSTVAKT_USERDB);
        EntityHandlerIface edao = (EntityHandlerIface) dao;
        try {
            edao.getAccount(username);
        }
        catch (EmptyResultException e) {
            // do nothing
        }
        catch (KustvaktException ex) {
            assertNull("Test user could not be set up", true);
        }

        Map m = new HashMap<>();
        m.put(Attributes.USERNAME, username);

        try {
            String hash = ((EncryptionIface) getBean(ContextHolder.KUSTVAKT_ENCRYPTION))
                    .secureHash(password);
            m.put(Attributes.PASSWORD, hash);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException
                | KustvaktException e) {
            // do nohting
            assertNotNull("Exception thrown", null);
        }
        assertNotNull("userdatabase handler must not be null", dao);

        try {

            int i = edao.createAccount(User.UserFactory.toKorAPUser(m));
            assert BeansFactory.getKustvaktContext().getUserDBHandler()
                    .getAccount(credentials[0]) != null;
            assertEquals(1, i);
        }
        catch (KustvaktException e) {
            // do nothing
            assertNull("Test user could not be set up", true);
        }
        return this;
    }


    public User getUser () {
        try {
            return ((EntityHandlerIface) getBean(ContextHolder.KUSTVAKT_USERDB))
                    .getAccount(credentials[0]);
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
        System.out.println(queries);
        for (String query : queries)
            source.update(query, new HashMap<String, Object>());
        return this;
    }


    public static final String[] getUserCredentials () {
        return Arrays.copyOf(credentials, 2);
    }


    public TestHelper runBootInterfaces () {
        Set<Class<? extends BootableBeanInterface>> set = KustvaktClassLoader
                .loadSubTypes(BootableBeanInterface.class);

        List<BootableBeanInterface> list = new ArrayList<>(set.size());
        for (Class cl : set) {
            BootableBeanInterface iface;
            try {
                iface = (BootableBeanInterface) cl.newInstance();
                list.add(iface);
            }
            catch (InstantiationException | IllegalAccessException e) {
                // do nothing
            }
        }
        jlog.debug("Found boot loading interfaces: " + list);
        while (!set.isEmpty()) {
            out_loop: for (BootableBeanInterface iface : new ArrayList<>(list)) {
                try {
                    jlog.debug("Running boot instructions from class "
                            + iface.getClass().getSimpleName());
                    for (Class cl : iface.getDependencies()) {
                        if (set.contains(cl))
                            continue out_loop;
                    }
                    set.remove(iface.getClass());
                    list.remove(iface);
                    iface.load(beansHolder);
                }
                catch (KustvaktException e) {
                    // don't do anything!
                    System.out.println("An error occurred in class "
                            + iface.getClass().getSimpleName() + "!\n" + e);
                    throw new RuntimeException(
                            "Boot loading interface failed ...");
                }
            }
        }
        return this;
    }


    public int setupResource (KustvaktResource resource)
            throws KustvaktException {
        ResourceDao dao = new ResourceDao(
                (PersistenceClient) getBean(ContextHolder.KUSTVAKT_DB));
        return dao.storeResource(resource, getUser());
    }


    public KustvaktResource getResource (String name) throws KustvaktException {
        ResourceDao dao = new ResourceDao(
                (PersistenceClient) getBean(ContextHolder.KUSTVAKT_DB));
        KustvaktResource res = dao.findbyId(name, getUser());
        if (res == null)
            throw new RuntimeException("resource with name " + name
                    + " not found ...");
        return res;
    }


    public TestHelper dropResource (String ... names) throws KustvaktException {
        ResourceDao dao = new ResourceDao(
                (PersistenceClient) getBean(ContextHolder.KUSTVAKT_DB));
        if (names == null || names.length == 0)
            dao.truncate();
        for (String name : names)
            dao.deleteResource(name, null);
        return this;
    }


    public void close () {
        BeansFactory.closeApplication();
    }


    private TestHelper () {

    }


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


    private static PersistenceClient sqlite_db (boolean memory)
            throws InterruptedException {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        DateTime t = new DateTime();
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


    public static PersistenceClient sqlite_db_norm (boolean memory) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setMaxTotal(1);
        dataSource.setInitialSize(1);
        dataSource.setMaxIdle(1);
        dataSource.addConnectionProperty("lazy-init", "true");
        DateTime t = new DateTime();
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

    public static class AppTestConfig extends TestBeans {

        public AppTestConfig () throws InterruptedException, IOException {
            this.dataSource = TestHelper.sqlite_db(true);
            //this.dataSource = TestHelper.mysql_db();
        }


        @Bean(name = ContextHolder.KUSTVAKT_POLICIES)
        @Override
        public PolicyHandlerIface getPolicyDao () {
            return new PolicyDao(this.dataSource);
        }


        @Bean(name = ContextHolder.KUSTVAKT_USERDB)
        @Override
        public EntityHandlerIface getUserDao () {
            return new EntityDao(this.dataSource);
        }


        @Bean(name = ContextHolder.KUSTVAKT_CONFIG)
        @Override
        public KustvaktConfiguration getConfig () {
            KustvaktConfiguration c = new KustvaktConfiguration();
            InputStream s = TestHelper.class.getClassLoader()
                    .getResourceAsStream("kustvakt.conf");
            if (s != null)
                c.setPropertiesAsStream(s);
            else {
                System.out.println("No properties found!");
                System.exit(-1);
            }
            return c;
        }


        @Bean(name = ContextHolder.KUSTVAKT_AUDITING)
        @Override
        public AuditingIface getAuditingDao () {
            return new JDBCAuditing(this.dataSource);
        }


        @Bean(name = ContextHolder.KUSTVAKT_RESOURCES)
        @Override
        public List<ResourceOperationIface> getResourceDaos () {
            List<ResourceOperationIface> res = new ArrayList<>();
            res.add(new ResourceDao(getDataSource()));
            res.add(new DocumentDao(getDataSource()));
            return res;
        }


        @Bean(name = ContextHolder.KUSTVAKT_USERDATA)
        @Override
        public List<UserDataDbIface> getUserdataDaos () {
            List<UserDataDbIface> ud = new ArrayList<>();
            ud.add(new UserSettingsDao(getDataSource()));
            ud.add(new UserDetailsDao(getDataSource()));
            return ud;
        }


        @Bean(name = ContextHolder.KUSTVAKT_ENCRYPTION)
        @Override
        public EncryptionIface getCrypto () {
            return new KustvaktEncryption(getConfig());
        }


        @Bean(name = ContextHolder.KUSTVAKT_AUTHENTICATION_MANAGER)
        @Override
        public AuthenticationManagerIface getAuthManager () {
            AuthenticationManagerIface manager = new KustvaktAuthenticationManager(
                    getUserDao(), getCrypto(), getConfig(), getAuditingDao(),
                    getUserdataDaos());
            Set<AuthenticationIface> pro = new HashSet<>();
            pro.add(new BasicHttpAuth());
            pro.add(new APIAuthentication(getConfig()));
            manager.setProviders(pro);
            return manager;
        }

    }

}
