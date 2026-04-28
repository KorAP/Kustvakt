package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * Tests that the LargeContextGroup is correctly created during
 * initialization (see {@link de.ids_mannheim.korap.init.Initializator#initTest()}).
 *
 * @author auto-generated
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
public class LargeContextGroupTest extends DaoTestBase {

    private static final String LARGE_CONTEXT_GROUP_NAME = "LargeContextGroup";
    private static final String LARGE_CONTEXT_GROUP_ADMIN = "korap_admin";

    @Test
    public void testLargeContextGroupExists () throws KustvaktException {
        UserGroup group = userGroupDao.retrieveGroupByName(
                LARGE_CONTEXT_GROUP_NAME, false);
        assertNotNull(group, "LargeContextGroup should exist after initialization");
        assertEquals(LARGE_CONTEXT_GROUP_NAME, group.getName());
        assertEquals(LARGE_CONTEXT_GROUP_ADMIN, group.getCreatedBy());
        assertEquals(UserGroupStatus.ACTIVE, group.getStatus());
    }
}