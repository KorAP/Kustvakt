package de.ids_mannheim.korap.resources;

/**
 * @author hanl
 * @date 21/01/2014
 */
public class Permissions {

   public static enum PERMISSIONS {
        //fixme: add read_policy permission to allow read policy permissions
        READ, WRITE, DELETE, READ_POLICY, CREATE_POLICY, MODIFY_POLICY, DELETE_POLICY
    }

    public static final byte READ = 1;
    public static final byte WRITE = 2;
    public static final byte DELETE = 4;
    public static final byte READ_POLICY = 8;
    public static final byte CREATE_POLICY = 16;
    public static final byte MODIFY_POLICY = 32;
    public static final byte DELETE_POLICY = 64;


    public static Byte getByte(PERMISSIONS perm) {
        switch (perm) {
            case READ:
                return READ;
            case WRITE:
                return WRITE;
            case DELETE:
                return DELETE;
            case READ_POLICY:
                return READ_POLICY;
            case DELETE_POLICY:
                return DELETE_POLICY;
            case MODIFY_POLICY:
                return MODIFY_POLICY;
            case CREATE_POLICY:
                return CREATE_POLICY;
            default:
                return 0;
        }
    }


}
