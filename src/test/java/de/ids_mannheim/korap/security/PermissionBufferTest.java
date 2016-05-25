package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.resources.Permissions;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 15/02/2016
 */
public class PermissionBufferTest {

    @Test
    public void testDuplicatePermission () {
        PermissionsBuffer buffer = new PermissionsBuffer();
        assertEquals(0, buffer.addPermission(4));
        assertEquals(-1, buffer.addPermission(4));

        // 0 means permission was successfully added, -1 means it wasn't because it's already present
        assertEquals(0, buffer.addPermission(1));
        assertEquals(-1, buffer.addPermission(1));

        assertEquals(0, buffer.addPermission(8));
        assertEquals(-1, buffer.addPermission(4));
    }


    @Test
    public void testPermissionsAdd () {
        PermissionsBuffer buffer = new PermissionsBuffer();
        buffer.addPermissions(Permissions.Permission.READ);
        assertEquals(1, buffer.getPermissions().size());

        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer
                .containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.ALL.toByte()));

        buffer = new PermissionsBuffer();
        buffer.addPermissions(Permissions.Permission.WRITE);
        buffer.addPermissions(Permissions.Permission.DELETE_POLICY);
        assertEquals(2, buffer.getPermissions().size());
        assertFalse(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer
                .containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.ALL.toByte()));
    }


    @Test
    public void testPermissionsAddAll2 () {
        PermissionsBuffer buffer = new PermissionsBuffer();
        buffer.addPermissions(Permissions.Permission.ALL);

        assertEquals(Permissions.Permission.values().length, buffer
                .getPermissions().size());
        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.ALL.toByte()));
    }


    @Test
    public void testPermissionsAddAll () {
        PermissionsBuffer buffer = new PermissionsBuffer();
        buffer.addPermissions(Permissions.Permission.DELETE_POLICY);
        buffer.addPermissions(Permissions.Permission.CREATE_POLICY);
        buffer.addPermissions(Permissions.Permission.READ);
        buffer.addPermissions(Permissions.Permission.MODIFY_POLICY);
        buffer.addPermissions(Permissions.Permission.DELETE);
        buffer.addPermissions(Permissions.Permission.READ_POLICY);
        buffer.addPermissions(Permissions.Permission.WRITE);

        assertEquals(Permissions.Permission.values().length, buffer
                .getPermissions().size());
        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.ALL.toByte()));

    }


    @Test
    public void testPermissionsInit () {
        PermissionsBuffer buffer = new PermissionsBuffer((short) 1);
        assertEquals(1, buffer.getPermissions().size());

        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertFalse(buffer
                .containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));

    }


    @Test
    public void testPermissionsStringConversion () {
        PermissionsBuffer buffer = new PermissionsBuffer(Short.valueOf("1"));
        assertEquals(1, buffer.getPermissions().size());

        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertFalse(buffer
                .containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));

        buffer = new PermissionsBuffer(Short.valueOf("5"));
        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));

        buffer = new PermissionsBuffer(Short.valueOf("69"));
        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.READ_POLICY
                .toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.CREATE_POLICY
                .toByte()));
    }


    @Test
    public void testPermissionRemovalOne () {
        PermissionsBuffer buffer = new PermissionsBuffer();
        buffer.addPermissions(Permissions.Permission.READ,
                Permissions.Permission.DELETE,
                Permissions.Permission.MODIFY_POLICY);

        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));

        buffer.removePermission(Permissions.Permission.MODIFY_POLICY);

        assertTrue(buffer.containsPByte(Permissions.Permission.READ.toByte()));
        assertTrue(buffer.containsPByte(Permissions.Permission.DELETE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.WRITE.toByte()));
        assertFalse(buffer.containsPByte(Permissions.Permission.MODIFY_POLICY
                .toByte()));
    }

}
