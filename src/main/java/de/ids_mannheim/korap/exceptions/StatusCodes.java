package de.ids_mannheim.korap.exceptions;

/**
 * @author hanl
 * @date 07/09/2014
 */
public class StatusCodes {

    /**
     * 100 status codes for standard system errors
     */
    public static final Integer EMPTY_RESULTS = 100;
    public static final Integer REQUEST_INVALID = 101;
    //fixme: redundancy?!
    public static final Integer ENTRY_EXISTS = 102;
    public static final Integer STATUS_OK = 103;
    public static final Integer UNSUPPORTED_OPERATION = 104;
    public static final Integer ILLEGAL_ARGUMENT = 105;
    public static final Integer CONNECTION_ERROR = 106;
    public static final Integer NOTHING_CHANGED = 107;
    public static final Integer PARAMETER_VALIDATION_ERROR = 108;
    public static final Integer DEFAULT_ERROR = 109;
    public static final Integer NOT_SUPPORTED = 110;

    /**
     * 400 status codes for account/authentication relevant components
     */

    public static final Integer ACCOUNT_DEACTIVATED = 200;
    public static final Integer ACCOUNT_CONFIRMATION_FAILED = 201;
    public static final Integer ALREADY_LOGGED_IN = 202;
    public static final Integer EXPIRED = 204;
    public static final Integer BAD_CREDENTIALS = 205;
    public static final Integer UNCONFIRMED_ACCOUNT = 206;
    public static final Integer NAME_EXISTS = 207;
    public static final Integer PASSWORD_RESET_FAILED = 208;
    // fixme: ?!
    @Deprecated
    public static final Integer AUTHENTICATION_DENIED = 209;

    public static final Integer LOGIN_SUCCESSFUL = 210;
    public static final Integer LOGIN_FAILED = 211;
    public static final Integer LOGOUT_SUCCESSFUL = 212;
    public static final Integer LOGOUT_FAILED = 213;

    public static final Integer CLIENT_REGISTRATION_FAILURE = 214;
    public static final Integer CLIENT_REMOVAL_FAILURE = 215;
    public static final Integer CLIENT_AUTHORIZATION_FAILURE = 216;

    /**
     * 500 status codes for access control related components (also policy rewrite)
     */
    public static final Integer PERMISSION_DENIED = 401;
    public static final Integer UNSUPPORTED_RESOURCE = 402;
    public static final Integer UNSUPPORTED_FOUNDRY = 403;
    public static final Integer UNSUPPORTED_CORPUS = 404;
    public static final Integer UNSUPPORTED_LAYER = 405;
    // make a distinction between no and invalid vc?
    public static final Integer UNSUPPORTED_COLLECTION = 406;
    public static final Integer CORPUS_REWRITE = 407;
    public static final Integer FOUNDRY_REWRITE = 408;
    public static final Integer FOUNDRY_INJECTION = 409;
    public static final Integer MISSING_ARGUMENTS = 410;
    public static final Integer MISSING_VIRTUALCOLLECTION = 411;
    public static final Integer MISSING_POLICY_TARGET = 412;
    public static final Integer MISSING_POLICY_CONDITIONS = 413;
    public static final Integer MISSING_POLICY_PERMISSION = 414;
    public static final Integer RESOURCE_NOT_FOUND = 415;

    // todo: extend according to policy rewrite possible!
    // policy errors
    public static final Integer POLICY_ERROR_DEFAULT = 500;
    public static final Integer POLICY_CREATE_ERROR = 501;
    public static final Integer NO_POLICIES = 502;

    // database codes
    public static final Integer DB_GET_FAILED = 601;
    public static final Integer DB_INSERT_FAILED = 602;
    public static final Integer DB_DELETE_FAILED = 603;
    public static final Integer DB_UPDATE_FAILED = 604;

    public static final Integer DB_GET_SUCCESSFUL = 605;
    public static final Integer DB_INSERT_SUCCESSFUL = 606;
    public static final Integer DB_DELETE_SUCCESSFUL = 607;
    public static final Integer DB_UPDATE_SUCCESSFUL = 608;

    // service status codes
    public static final Integer CREATE_ACCOUNT_SUCCESSFUL = 700;
    public static final Integer CREATE_ACCOUNT_FAILED = 701;
    public static final Integer DELETE_ACCOUNT_SUCCESSFUL = 702;
    public static final Integer DELETE_ACCOUNT_FAILED = 703;
    public static final Integer UPDATE_ACCOUNT_SUCCESSFUL = 704;
    public static final Integer UPDATE_ACCOUNT_FAILED = 705;

    public static final Integer GET_ACCOUNT_SUCCESSFUL = 706;
    public static final Integer GET_ACCOUNT_FAILED = 707;

    /**
     * 300 status codes for query language and serialization
     */

    public static final Integer NO_QUERY = 301;

}
