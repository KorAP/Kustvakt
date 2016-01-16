-- rename all columns in new way!
CREATE TABLE IF NOT EXISTS korap_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    account_lock boolean NOT NULL,
    account_creation BIGINT NOT NULL,
    type INTEGER DEFAULT 0,
    uri_fragment VARCHAR(100),
    uri_expiration BIGINT,
    loginSuccess INTEGER,
    loginFailed INTEGER,
    account_link VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS shib_users (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    account_creation BIGINT NOT NULL,
    type INTEGER DEFAULT 1,
    loginSuccess INTEGER,
    loginFailed INTEGER,
    account_link VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user_details (
    Id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    firstName VARCHAR(100),
    lastName VARCHAR(100),
    gender VARCHAR(100),
    phone VARCHAR(100),
    institution VARCHAR(100),
    email VARCHAR(100),
    address VARCHAR(100),
    country VARCHAR(100),
    privateUsage BOOLEAN,
    foreign key (user_id)
    references korap_users (id)
    on delete cascade
);

CREATE TABLE IF NOT EXISTS user_settings (
    Id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    fileNameForExport VARCHAR(100),
    itemForSimpleAnnotation INTEGER,
    leftContextItemForExport VARCHAR(100),
    leftContextSizeForExport INTEGER,
    locale VARCHAR(100),
    leftContextItem VARCHAR(100),
    leftContextSize INTEGER,
    rightContextItem VARCHAR(100),
    rightContextItemForExport VARCHAR(100),
    rightContextSize INTEGER,
    rightContextSizeForExport INTEGER,
    selectedCollection VARCHAR(100),
    queryLanguage VARCHAR(100),
    pageLength INTEGER,
    metadataQueryExpertModus BOOLEAN,
    searchSettingsTab INTEGER,
    selectedGraphType INTEGER,
    selectedSortType VARCHAR(100),
    selectedViewForSearchResults VARCHAR(100),
    POSFoundry VARCHAR(100),
    lemmaFoundry VARCHAR(100),
    constFoundry VARCHAR(100),
    relFoundry VARCHAR(100),
    collectData BOOLEAN,
    foreign key (user_id)
    references korap_users (id)
    on delete cascade
);

-- deprecated
CREATE OR REPLACE VIEW allusers AS
    SELECT
        id,
        username,
        password,
        account_lock,
        account_creation,
        type,
        uri_fragment,
        uri_expiration,
        loginSuccess,
        loginFailed,
        account_link
    from
        korap_users
    UNION ALL SELECT
        id,
        username,
        NULL as password,
        NULL as account_lock,
        account_creation,
        type,
        NULL as uri_fragment,
        NULL as uri_expiration,
        loginSuccess,
        loginFailed,
        account_link
    from
        shib_users;
