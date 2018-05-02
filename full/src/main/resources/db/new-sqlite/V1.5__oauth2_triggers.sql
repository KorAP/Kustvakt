CREATE TRIGGER insert_authorization_date AFTER INSERT ON oauth2_authorization
     BEGIN
      UPDATE oauth2_authorization
      SET created_date = DATETIME('now', 'localtime')  
      WHERE rowid = new.rowid;
     END;

CREATE TRIGGER insert_access_token_date AFTER INSERT ON oauth2_access_token
     BEGIN
      UPDATE oauth2_access_token
      SET created_date = DATETIME('now', 'localtime')  
      WHERE rowid = new.rowid;
     END;
     