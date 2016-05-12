package de.ids_mannheim.korap.resources;

/**
 * @author hanl
 * @date 21/01/2014
 */
public class Permissions {

    public enum Permission {

        //fixme: add read_policy permission to allow read policy permissions
        READ(Permissions.READ),
        WRITE(Permissions.WRITE),
        DELETE(Permissions.DELETE),

        READ_POLICY(Permissions.READ_POLICY),
        CREATE_POLICY(Permissions.CREATE_POLICY),
        MODIFY_POLICY(Permissions.MODIFY_POLICY),
        DELETE_POLICY(Permissions.DELETE_POLICY),
        ALL(Permissions.ALL);

        private final byte b;

        Permission(byte b) {
            this.b = b;
        }

        public byte toByte() {
            return this.b;
        }
    }

    private static final byte READ = 1;
    private static final byte WRITE = 2;
    private static final byte DELETE = 4;
    private static final byte READ_POLICY = 8;
    private static final byte CREATE_POLICY = 16;
    private static final byte MODIFY_POLICY = 32;
    private static final byte DELETE_POLICY = 64;
    private static final byte ALL = 127;

}