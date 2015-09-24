package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.resources.Permissions;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * User: hanl
 * Date: 11/5/13
 * Time: 1:05 PM
 */
public class PermissionsBuffer {

    private byte[] bytes;

    public PermissionsBuffer() {
        this((short) 0);
    }

    public PermissionsBuffer(short perm) {
        setByte(perm);
    }

    private void setByte(short perm) {
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort(perm);
        bytes = b.array();
    }

    public PermissionsBuffer(byte... bytes) {
        this.bytes = bytes;
    }

    public boolean containsPermission(Permissions.PERMISSIONS p) {
        return containsPByte(Permissions.getByte(p));
    }

    public boolean containsPByte(byte perm) {
        return (bytes[1] & perm) == perm;
    }

    public int addPermission(int b) {
        short r = (short) (bytes[1] & b);
        if ((bytes[1] & b) != b)
            bytes[1] += (b - r);
        else
            return -1;
        return 0;
    }

    public void retain(int compare) {
        short f = (short) (bytes[1] & compare);
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort(f);
        bytes = b.array();
    }

    public void addPermissions(Permissions.PERMISSIONS... perm) {
        if (perm.length > 0) {
            for (Permissions.PERMISSIONS p : perm)
                addPermission(Permissions.getByte(p));
        }
    }

    public void removePermission(Permissions.PERMISSIONS perm) {
        this.removePermission(Permissions.getByte(perm));
    }


    public int removePermission(int b) {
        if ((bytes[1] & b) != 0)
            bytes[1] -= b;
        else
            return -1;
        return 0;
    }

    @Deprecated
    public int addOverride(int b) {
        if ((bytes[0] & b) == 0)
            bytes[0] += b;
        else
            return -1;
        return 0;
    }

    public int removeOverride(int b) {
        if ((bytes[0] & b) != 0)
            bytes[0] -= b;
        else
            return -1;
        return 0;
    }

    @Deprecated
    public boolean isOverridable(int b) {
        return (bytes[0] & b) != 0;
    }

    public boolean leftShift(byte perm) {
//        return pbyte & (perm << 1);
        System.out.println("pbyte is: " + bytes[1]);
        System.out.println("bitswise operation, left shift " + (perm << 1));
        return false;
    }

    @Override
    public boolean equals(Object perm) {
        if (perm instanceof Byte)
            return (bytes[1] & (byte) perm) == bytes[1];
        else if (perm instanceof PermissionsBuffer) {
            PermissionsBuffer b = (PermissionsBuffer) perm;
            return (bytes[1] & b.bytes[1]) == bytes[1];
        }
        return false;
    }

    public short getBytes() {
        ByteBuffer b = ByteBuffer.wrap(bytes);
        return b.getShort();
    }

    public Byte getPbyte() {
        return this.bytes[1];
    }

    public Set<Permissions.PERMISSIONS> getPermissions() {
        Set<Permissions.PERMISSIONS> pe = new HashSet<>();
        if (containsPByte(Permissions.READ))
            pe.add(Permissions.PERMISSIONS.READ);
        if (containsPByte(Permissions.WRITE))
            pe.add(Permissions.PERMISSIONS.WRITE);
        if (containsPByte(Permissions.DELETE))
            pe.add(Permissions.PERMISSIONS.DELETE);
        if (containsPByte(Permissions.CREATE_POLICY))
            pe.add(Permissions.PERMISSIONS.CREATE_POLICY);
        if (containsPByte(Permissions.MODIFY_POLICY))
            pe.add(Permissions.PERMISSIONS.MODIFY_POLICY);
        if (containsPByte(Permissions.DELETE_POLICY))
            pe.add(Permissions.PERMISSIONS.DELETE_POLICY);
        return pe;
    }


    public byte getOverride() {
        return this.bytes[0];
    }

    public String toBinary() {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for (int i = 0; i < Byte.SIZE * bytes.length; i++) {
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        }
        return sb.toString();
    }


}
