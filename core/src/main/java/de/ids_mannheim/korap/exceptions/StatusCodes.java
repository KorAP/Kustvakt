package de.ids_mannheim.korap.exceptions;

import java.util.Properties;

import de.ids_mannheim.korap.config.ConfigLoader;

/**
 * @author hanl
 * @date 07/09/2014
 */
public class StatusCodes {

    /**
     * 100 status codes for standard system errors
     */
    public static final int DEFAULT_ERROR = 100;
    public static final int NO_RESULT_FOUND = 101;

    public static final int UNSUPPORTED_OPERATION = 103;
    public static final int ILLEGAL_ARGUMENT = 104;
    public static final int MISSING_ARGUMENT = 105;
    public static final int CONNECTION_ERROR = 106;
    public static final int INVALID_ARGUMENT = 107;
    public static final int NOT_SUPPORTED = 108;
    public static final int NOT_ALLOWED = 109;
    
    /**
     * 300 status codes for query language and serialization
     */

    public static final int NO_QUERY = 301;
//    public static final int INVALID_TYPE = 302;
    public static final int MISSING_ATTRIBUTE = 303;
    public static final int INVALID_ATTRIBUTE = 304;
    public static final int UNSUPPORTED_VALUE = 305;
    public static final int SERIALIZATION_FAILED = 306;
    public static final int DESERIALIZATION_FAILED = 307;
    
    /**
     *  400 status codes for authorization and rewrite functions
     */

    // fixme: use unsupported resource and include type in return message
    public static final int POLICY_ERROR_DEFAULT = 400;
    
    public static final int UNSUPPORTED_RESOURCE = 402;
//    public static final int REWRITE_FAILED = 403;
    //public static final int UNSUPPORTED_FOUNDRY = 403;
    //public static final int UNSUPPORTED_CORPUS = 404;
    //public static final int UNSUPPORTED_LAYER = 405;
    // make a distinction between no and invalid vc?
    //public static final int UNSUPPORTED_COLLECTION = 406;
    //public static final int CORPUS_REWRITE = 407;
    //public static final int FOUNDRY_REWRITE = 408;
    //public static final int FOUNDRY_INJECTION = 409;
//    public static final int MISSING_RESOURCE = 405;
    public static final int NO_POLICY_TARGET = 406;
    public static final int NO_POLICY_CONDITION = 407;
    public static final int NO_POLICY_PERMISSION = 408;
    public static final int NO_POLICIES = 409;

    
    
    /**
     * 500 status codes for access control related components (also
     * policy rewrite)
     */
    // todo: extend according to policy rewrite possible!
    // policy errors


    // database codes
    public static final int DB_GET_FAILED = 500;
    public static final int DB_INSERT_FAILED = 501;
    public static final int DB_DELETE_FAILED = 502;
    public static final int DB_UPDATE_FAILED = 503;

    public static final int DB_GET_SUCCESSFUL = 504;
    public static final int DB_INSERT_SUCCESSFUL = 505;
    public static final int DB_DELETE_SUCCESSFUL = 506;
    public static final int DB_UPDATE_SUCCESSFUL = 507;
    public static final int DB_ENTRY_EXISTS = 508;
    
    
    // User group and member 
    public static final int GROUP_MEMBER_EXISTS = 601;
    public static final int GROUP_MEMBER_DELETED = 602;
    public static final int INVITATION_EXPIRED = 602;
    

//    public static final int ARGUMENT_VALIDATION_FAILURE = 700;
    // public static final int ARGUMENT_VALIDATION_FAILURE = 701;

    // service status codes
    public static final int CREATE_ACCOUNT_SUCCESSFUL = 700;
    public static final int CREATE_ACCOUNT_FAILED = 701;
    public static final int DELETE_ACCOUNT_SUCCESSFUL = 702;
    public static final int DELETE_ACCOUNT_FAILED = 703;
    public static final int UPDATE_ACCOUNT_SUCCESSFUL = 704;
    public static final int UPDATE_ACCOUNT_FAILED = 705;

    public static final int GET_ACCOUNT_SUCCESSFUL = 706;
    public static final int GET_ACCOUNT_FAILED = 707;
    
    
    public static final int STATUS_OK = 1000;
    public static final int NOTHING_CHANGED = 1001;
    public static final int REQUEST_INVALID = 1002;
    public static final int ACCESS_DENIED = 1003;

    /**
     * 1800 Oauth2
     */

    public static final int CLIENT_REGISTRATION_FAILED = 1800;
    public static final int CLIENT_REMOVAL_FAILURE = 1801;
    
    /**
     * 1900 User account and logins
     */

    public static final int LOGIN_SUCCESSFUL = 1900;
    public static final int ALREADY_LOGGED_IN = 1901;
    
    public static final int LOGOUT_SUCCESSFUL = 1902;
    public static final int LOGOUT_FAILED = 1903;
    
    public static final int ACCOUNT_CONFIRMATION_FAILED = 1904;
    public static final int PASSWORD_RESET_FAILED = 1905;
    
    /**
     * 2000 status and error codes concerning authentication
     * 
     * Response with WWW-Authenticate header will be created 
     * for all KustvaktExceptions with status codes 2000 or greater  
     *  
     * MH: service level messages and callbacks
     */

    public static final int AUTHENTICATION_FAILED = 2000;
    public static final int LOGIN_FAILED = 2001;
    public static final int EXPIRED = 2002;
    public static final int BAD_CREDENTIALS = 2003;
    public static final int ACCOUNT_NOT_CONFIRMED = 2004;
    public static final int ACCOUNT_DEACTIVATED = 2005;

//    public static final int CLIENT_AUTHORIZATION_FAILED = 2013;
    public static final int AUTHORIZATION_FAILED = 2010;
    
    // 2020 - 2029 reserviert für LDAP-Fehlercodes - 21.04.17/FB
    public static final int LDAP_BASE_ERRCODE = 2020;
    
    /**/
    private static StatusCodes codes;

    private final Properties props;

    private StatusCodes() {
        this.props = ConfigLoader.loadProperties("codes.info");
    }


    public static final String getMessage(int code) {
        return getCodes().props.getProperty(String.valueOf(code));
    }

    public static StatusCodes getCodes() {
            if (codes == null)
                codes = new StatusCodes();
        return codes;
    }

}
