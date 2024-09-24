package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.sqlite.SQLiteException;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import jakarta.persistence.PersistenceException;

@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
public class RoleDaoTest extends DaoTestBase {

    @Autowired
    private RoleDao roleDao;
    @Autowired
    private QueryDao queryDao;

    @Test
    public void testUniqueRoleWithoutQuery () throws KustvaktException {
        UserGroup group = createDoryGroup();

        Role r = new Role(PredefinedRole.GROUP_ADMIN, PrivilegeType.READ_MEMBER,
                group);

        Exception exception = assertThrows(PersistenceException.class, () -> {
            roleDao.addRole(r);
        });

        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        assertEquals(SQLiteException.class, rootCause.getClass());
        assertTrue(rootCause.getMessage()
                .startsWith("[SQLITE_CONSTRAINT_UNIQUE]"));

        deleteUserGroup(group.getId(), "dory");
    }

    @Test
    public void testUniqueRoleWithQuery () throws KustvaktException {
        QueryDO query = queryDao.retrieveQueryByName("dory-vc", "dory");

        UserGroup group = createDoryGroup();

        Role r1 = new Role(PredefinedRole.GROUP_ADMIN,
                PrivilegeType.READ_MEMBER, group, query);
        roleDao.addRole(r1);

        Role r2 = new Role(PredefinedRole.GROUP_ADMIN,
                PrivilegeType.READ_MEMBER, group, query);

        Exception exception = assertThrows(PersistenceException.class, () -> {
            roleDao.addRole(r2);
        });
        
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        assertEquals(SQLiteException.class, rootCause.getClass());
        assertTrue(rootCause.getMessage()
                .startsWith("[SQLITE_CONSTRAINT_UNIQUE]"));

        deleteUserGroup(group.getId(), "dory");
    }
}
