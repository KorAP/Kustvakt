package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.interfaces.db.PersistenceClient;
import de.ids_mannheim.korap.user.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.web.service.BootupInterface;
import org.junit.Assert;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * creates a test user that can be used to access protected functions
 *
 * @author hanl
 * @date 16/10/2015
 */
public class TestHelper {

    private static final String[] credentials = new String[] { "test1",
            "testPass2015" };

    public static boolean setupAccount() {
        boolean r = BeanConfiguration.hasContext();
        if (r && BeanConfiguration.getBeans().getUserDBHandler().size() == 0) {
            EntityHandlerIface dao = BeanConfiguration.getBeans()
                    .getUserDBHandler();
            Map m = new HashMap<>();
            m.put(Attributes.USERNAME, credentials[0]);
            m.put(Attributes.PASSWORD, credentials[1]);
            m.put(Attributes.FIRSTNAME, "test");
            m.put(Attributes.LASTNAME, "user");
            m.put(Attributes.EMAIL, "test@ids-mannheim.de");
            m.put(Attributes.ADDRESS, "Mannheim");

            Assert.assertNotNull("userdatabase handler must not be null", dao);

            try {
                BeanConfiguration.getBeans().getAuthenticationManager()
                        .createUserAccount(m, false);
            }catch (KustvaktException e) {
                // do nothing
                e.printStackTrace();
                Assert.assertNull("Test user could not be set up", true);
                return false;
            }
        }
        return r;
    }

    public static boolean setupSimpleAccount(String username, String password) {
        boolean r = BeanConfiguration.hasContext();
        if (r && BeanConfiguration.getBeans().getUserDBHandler().size() == 0) {
            EntityHandlerIface dao = BeanConfiguration.getBeans()
                    .getUserDBHandler();
            Map m = new HashMap<>();
            m.put(Attributes.USERNAME, username);

            try {
                String hash = BeanConfiguration.getBeans().getEncryption()
                        .produceSecureHash(password);
                m.put(Attributes.PASSWORD, hash);
            }catch (NoSuchAlgorithmException | UnsupportedEncodingException | KustvaktException e) {

            }
            Assert.assertNotNull("userdatabase handler must not be null", dao);

            try {
                int i = dao.createAccount(User.UserFactory.toKorAPUser(m));
                assert BeanConfiguration.getBeans().getUserDBHandler()
                        .getAccount(credentials[0]) != null;
                assert i == 1;
            }catch (KustvaktException e) {
                // do nothing
                Assert.assertNull("Test user could not be set up", true);
                return false;
            }
        }
        return r;
    }

    public static User getUser() {
        if (BeanConfiguration.hasContext()) {
            try {
                return BeanConfiguration.getBeans().getUserDBHandler()
                        .getAccount(credentials[0]);
            }catch (KustvaktException e) {
            }
        }
        throw new RuntimeException("User could not be retrieved!");
    }

    public static boolean dropUser(String... usernames) {
        if (usernames == null || usernames.length == 0)
            usernames = new String[] { credentials[0] };
        if (BeanConfiguration.hasContext()) {
            for (String name : usernames)
                remove(name);
        }
        return BeanConfiguration.hasContext();
    }

    private static void remove(String username) {
        EntityHandlerIface dao = BeanConfiguration.getBeans()
                .getUserDBHandler();
        try {
            User us = dao.getAccount(username);
            dao.deleteAccount(us.getId());
        }catch (KustvaktException e) {
            // do nothing
        }
    }

    public static void drop() {
        if (BeanConfiguration.hasContext()) {
            PersistenceClient cl = BeanConfiguration.getBeans()
                    .getPersistenceClient();
            String sql = "drop database " + cl.getDatabase() + ";";
            NamedParameterJdbcTemplate jdbc = (NamedParameterJdbcTemplate) cl
                    .getSource();
            jdbc.update(sql, new HashMap<String, Object>());
        }
    }

    public static boolean truncateAll() {
        boolean r = BeanConfiguration.hasContext();
        if (r) {
            String sql = "SELECT Concat('TRUNCATE TABLE ', TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES";
            final Set<String> queries = new HashSet<>();
            PersistenceClient cl = BeanConfiguration.getBeans()
                    .getPersistenceClient();
            NamedParameterJdbcTemplate source = (NamedParameterJdbcTemplate) cl
                    .getSource();

            source.query(sql, new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    queries.add(rs.getString(1));

                }
            });
            System.out.println(queries);
            for (String query : queries)
                source.update(query, new HashMap<String, Object>());
        }
        return r;
    }

    public static final String[] getUserCredentials() {
        return Arrays.copyOf(credentials, 2);
    }

    public static void runBootInterfaces() {
        Set<Class<? extends BootupInterface>> set = KustvaktClassLoader
                .loadSubTypes(BootupInterface.class);

        List<BootupInterface> list = new ArrayList<>(set.size());

        PersistenceClient client = BeanConfiguration.getBeans()
                .getPersistenceClient();
        if (client.checkDatabase()) {
            for (Class cl : set) {
                BootupInterface iface;
                try {
                    iface = (BootupInterface) cl.newInstance();
                    if (iface.position() == -1 | iface.position() > set.size())
                        list.add(iface);
                    else
                        list.add(0, iface);
                }catch (InstantiationException | IllegalAccessException e) {
                    continue;
                }
            }
            System.out.println("Found boot loading interfaces: " + list);
            for (BootupInterface iface : list) {
                try {
                    iface.load();
                }catch (KustvaktException e) {
                    // don't do anything!
                }
            }
        }else
            throw new RuntimeException("Client not setup properly!");
    }

    private TestHelper() {
    }

}
