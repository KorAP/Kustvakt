package de.ids_mannheim.korap.config;

public class Attributes {

	// EM: Use enum for the authentication types
    public static final String AUTHORIZATION = "Authorization";
//    public static final String SESSION_AUTHENTICATION = "session_token";
//    public static final String API_AUTHENTICATION = "api_token";
//    public static final String OAUTH2_AUTHORIZATION = "bearer";
//    public static final String OPENID_AUTHENTICATION = "id_token";
//    public static final String BASIC_AUTHENTICATION = "basic";

    public static final String LOCATION = "location"; // location of Client: User.INTERN/EXTERN
    public static final String CORPUS_ACCESS = "corpusAccess"; // User.ALL/PUB/FREE.
    
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String SCOPES = "scopes";

    public static final String PUBLIC_GROUP = "public";

    public static final String SERVICE_ACCESS = "service_access";
    public static final String USER = "KorapUser";
    public static final String SHIBUSER = "ShibUser";
    public static final String DEMO_DISPLAY = "Anonymous";
    public static final String DEMOUSER_PASSWORD = "demo";

    public static final String SETTINGS = "LocalSettings";
    //    public static final String STORAGE_SETTINGS = "StorageSettings";

    public static final String QUERY_ABBREVIATION = "Q";
    public static final String LAYER = "layer";

    public static final String TYPE = "type";

    public static final String ID = "ID";
    @Deprecated
    //refactor
    public static final String UID = "accountID";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String GENDER = "gender";
    public static final String FIRSTNAME = "firstName";
    public static final String LASTNAME = "lastName";
    public static final String PHONE = "phone";
    public static final String INSTITUTION = "institution";
    public static final String EMAIL = "email";
    public static final String ADDRESS = "address";
    public static final String COUNTRY = "country";
    public static final String IPADDRESS = "ipaddress";
    public static final String IS_ADMIN = "admin";
    // deprecated, use created
    public static final String ACCOUNT_CREATION = "account_creation";
    public static final String ACCOUNTLOCK = "account_lock";
    public static final String ACCOUNTLINK = "account_link";
    public static final String URI = "uri";
    public static final String URI_FRAGMENT = "uri_fragment";
    public static final String URI_EXPIRATION = "uri_expiration";
    public static final String PRIVATE_USAGE = "privateUsage";

    /**
     * token context
     */
    public static final String TOKEN = "token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String TOKEN_EXPIRATION = "expires";
    public static final String TOKEN_CREATION = "tokenCreated";
    public static final String USER_AGENT = "User-Agent";
    public static final String HOST = "userIP";

    public static final String QUERY_PARAM_URI = "uri";
    public static final String QUERY_PARAM_USER = "user";

    /**
     * shibboleth attribute names
     */
    public static final String EPPN = "eppn";
    public static final String COMMON_NAME = "cn";
    public static final String SURNAME = "sn";

    public static final String EDUPERSON = "eduPersonPrincipalName";
    public static final String CN = "cn";
    public static final String MAIL = "mail";
    public static final String EDU_AFFIL = "eduPersonScopedAffiliation";

    /**
     * resource mappings
     */

    public static final String RID = "id";
    public static final String OWNER = "owner";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";

    public static final String CORPUS_SIGLE = "corpusSigle";
    public static final String DOC_SIGLE = "docSigle";
    public static final String TEXT_SIGLE = "textSigle";

    public static final String AVAILABILITY = "availability";
    
    public static final String REF_CORPUS = "refCorpus";
    public static final String QUERY = "query";
    public static final String CACHE = "cache";
    public static final String DOCIDS = "docIDs";
    public static final String FOUNDRIES = "foundries";
    public static final String DEFAULT_VALUE = "defaultColl";

    public static final String FILE_FORMAT_FOR_EXPORT = "fileFormatForExport";
    public static final String FILENAME_FOR_EXPORT = "fileNameForExport";
    @Deprecated
    public static final String ITEM_FOR_SIMPLE_ANNOTATION = "itemForSimpleAnnotation";
    public static final String LEFT_CONTEXT_ITEM_FOR_EXPORT = "leftContextItemForExport";
    public static final String LEFT_CONTEXT_SIZE_FOR_EXPORT = "leftContextSizeForExport";
    public static final String LOCALE = "locale";
    public static final String LEFT_CONTEXT_ITEM = "leftContextItem";
    public static final String LEFT_CONTEXT_SIZE = "leftContextSize";
    public static final String RIGHT_CONTEXT_ITEM = "rightContextItem";
    public static final String RIGHT_CONTEXT_ITEM_FOR_EXPORT = "rightContextItemForExport";
    public static final String RIGHT_CONTEXT_SIZE = "rightContextSize";
    public static final String RIGHT_CONTEXT_SIZE_FOR_EXPORT = "rightContextSizeForExport";
    public static final String SELECTED_COLLECTION = "selectedCollection";
    public static final String QUERY_LANGUAGE = "queryLanguage";
    public static final String PAGE_LENGTH = "pageLength";
    public static final String METADATA_QUERY_EXPERT_MODUS = "metadataQueryExpertModus";
    @Deprecated
    public static final String SEARCH_SETTINGS_TAB = "searchSettingsTab";
    @Deprecated
    public static final String SELECTED_BROWSER_PROPERTY = "selectedBrowserProperty";
    @Deprecated
    public static final String SELECTED_CONTEXT_ITEM = "selectedContextItem";
    @Deprecated
    public static final String SELECTED_GRAPH_TYPE = "selectedGraphType";
    @Deprecated
    public static final String SELECTED_SORT_TYPE = "selectedSortType";
    @Deprecated
    public static final String SELECTED_VIEW_FOR_SEARCH_RESULTS = "selectedViewForSearchResults";
    public static final String COLLECT_AUDITING_DATA = "collectData";

    /**
     * default layers
     */
    public static final String DEFAULT_POS_FOUNDRY = "POSFoundry";
    public static final String DEFAULT_LEMMA_FOUNDRY = "lemmaFoundry";
    public static final String DEFAULT_CONST_FOUNDRY = "constFoundry";
    public static final String DEFAULT_REL_FOUNDRY = "relFoundry";

    /**
     * db column keys
     */

    public static final String SELF_REF = "self";

    public static final String SYM_USE = "sym_use";
    public static final String COMMERCIAL = "commercial";
    public static final String LICENCE = "licence";
    public static final String QUERY_ONLY = "query_only";
    public static final String EXPORT = "export";
    public static final String TIME_SPANS = "spans";
    public static final String RANGE = "range";

    public static final String GROUP_ID = "group_id";
    public static final String CREATED = "created";
    public static final String CREATOR = "creator";
    public static final String ENABLED = "enabled";
    public static final String EXPIRE = "expired";
    public static final String TARGET_ID = "target_id";
    public static final String IP_RANG = "ip_range";
    public static final String PERSISTENT_ID = "persistent_id";
    public static final String DISABLED = "disabled";
    public static final String USER_ID = "user_id";
    public static final String PARENT_ID = "parent_id";
    //    public static final String

}
