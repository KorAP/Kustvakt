package de.ids_mannheim.korap.constant;

/**
 * Defines all possible authorization scopes
 * 
 * @author margaretha
 *
 */
public enum OAuth2Scope {

    ALL, @Deprecated
    ADMIN,

    AUTHORIZE,

    LIST_USER_CLIENT, INSTALL_USER_CLIENT, UNINSTALL_USER_CLIENT,

    CLIENT_INFO, REGISTER_CLIENT, DEREGISTER_CLIENT, RESET_CLIENT_SECRET,

    SEARCH, SERIALIZE_QUERY, MATCH_INFO,

    USER_INFO,

    USER_GROUP_INFO, CREATE_USER_GROUP, DELETE_USER_GROUP,

    DELETE_USER_GROUP_MEMBER, ADD_USER_GROUP_MEMBER,

    ADD_USER_GROUP_MEMBER_ROLE, DELETE_USER_GROUP_MEMBER_ROLE,

    CREATE_VC, VC_INFO, DELETE_VC,

    SHARE_VC, DELETE_VC_ACCESS, VC_ACCESS_INFO,

    CREATE_DEFAULT_SETTING, READ_DEFAULT_SETTING, DELETE_DEFAULT_SETTING;

    @Override
    public String toString () {
        return super.toString().toLowerCase();
    }
}
