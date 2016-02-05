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
id INTEGER PRIMARY KEY AUTO_INCREMENT,
user_id INTEGER UNIQUE NOT NULL,
data BLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS user_settings (
id INTEGER PRIMARY KEY AUTO_INCREMENT,
user_id INTEGER UNIQUE NOT NULL,
data BLOB NOT NULL
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
