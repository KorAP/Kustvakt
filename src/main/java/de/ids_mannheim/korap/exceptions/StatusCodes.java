package de.ids_mannheim.korap.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ids_mannheim.korap.config.ConfigLoader;

import java.io.IOException;
import java.util.Properties;

/**
 * @author hanl
 * @date 07/09/2014
 */
public class StatusCodes {

    /**
     * 100 status codes for standard system errors
     */
    public static final int EMPTY_RESULTS = 100;
    public static final int REQUEST_INVALID = 101;
    //fixme: redundancy?!
    public static final int ENTRY_EXISTS = 102;
    public static final int STATUS_OK = 103;
    public static final int UNSUPPORTED_OPERATION = 104;
    public static final int ILLEGAL_ARGUMENT = 105;
    public static final int CONNECTION_ERROR = 106;
    public static final int NOTHING_CHANGED = 107;
    public static final int PARAMETER_VALIDATION_ERROR = 108;
    public static final int DEFAULT_ERROR = 109;
    public static final int NOT_SUPPORTED = 110;

    /**
     * 200 status codes for account/authentication relevant components
     */

    public static final int ACCOUNT_DEACTIVATED = 200;
    public static final int ACCOUNT_CONFIRMATION_FAILED = 201;
    public static final int ALREADY_LOGGED_IN = 202;
    public static final int EXPIRED = 204;
    public static final int BAD_CREDENTIALS = 205;
    @Deprecated // fixme: duplicate to account deactivated
    public static final int UNCONFIRMED_ACCOUNT = 206;
    public static final int NAME_EXISTS = 207;
    public static final int PASSWORD_RESET_FAILED = 208;
    // fixme: ?!
    @Deprecated
    public static final int AUTHENTICATION_DENIED = 209;

    public static final int LOGIN_SUCCESSFUL = 210;
    public static final int LOGIN_FAILED = 211;
    public static final int LOGOUT_SUCCESSFUL = 212;
    public static final int LOGOUT_FAILED = 213;

    public static final int CLIENT_REGISTRATION_FAILURE = 214;
    public static final int CLIENT_REMOVAL_FAILURE = 215;
    public static final int CLIENT_AUTHORIZATION_FAILURE = 216;


    /**
     *  400 status codes for authorization and rewrite functions
     */

    // fixme: use unsupported resource and include type in return message
    public static final int PERMISSION_DENIED = 401;
    public static final int UNSUPPORTED_RESOURCE = 402;
    public static final int UNSUPPORTED_FOUNDRY = 403;
    public static final int UNSUPPORTED_CORPUS = 404;
    public static final int UNSUPPORTED_LAYER = 405;
    // make a distinction between no and invalid vc?
    public static final int UNSUPPORTED_COLLECTION = 406;
    public static final int CORPUS_REWRITE = 407;
    public static final int FOUNDRY_REWRITE = 408;
    public static final int FOUNDRY_INJECTION = 409;
    public static final int MISSING_ARGUMENTS = 410;
    public static final int MISSING_VIRTUALCOLLECTION = 411;
    public static final int MISSING_POLICY_TARGET = 412;
    public static final int MISSING_POLICY_CONDITIONS = 413;
    public static final int MISSING_POLICY_PERMISSION = 414;
    public static final int RESOURCE_NOT_FOUND = 415;
    public static final int ACCESS_DENIED_NO_RESOURCES = 416;


    /**
     * 500 status codes for access control related components (also
     * policy rewrite)
     */
    // todo: extend according to policy rewrite possible!
    // policy errors
    public static final int POLICY_ERROR_DEFAULT = 500;
    public static final int POLICY_CREATE_ERROR = 501;
    public static final int NO_POLICIES = 502;

    // database codes
    public static final int DB_GET_FAILED = 601;
    public static final int DB_INSERT_FAILED = 602;
    public static final int DB_DELETE_FAILED = 603;
    public static final int DB_UPDATE_FAILED = 604;

    public static final int DB_GET_SUCCESSFUL = 605;
    public static final int DB_INSERT_SUCCESSFUL = 606;
    public static final int DB_DELETE_SUCCESSFUL = 607;
    public static final int DB_UPDATE_SUCCESSFUL = 608;

    // service status codes
    public static final int CREATE_ACCOUNT_SUCCESSFUL = 700;
    public static final int CREATE_ACCOUNT_FAILED = 701;
    public static final int DELETE_ACCOUNT_SUCCESSFUL = 702;
    public static final int DELETE_ACCOUNT_FAILED = 703;
    public static final int UPDATE_ACCOUNT_SUCCESSFUL = 704;
    public static final int UPDATE_ACCOUNT_FAILED = 705;

    public static final int GET_ACCOUNT_SUCCESSFUL = 706;
    public static final int GET_ACCOUNT_FAILED = 707;

    /**
     * 300 status codes for query language and serialization
     */

    public static final int NO_QUERY = 301;



    private StatusCodes() {

    }

}
