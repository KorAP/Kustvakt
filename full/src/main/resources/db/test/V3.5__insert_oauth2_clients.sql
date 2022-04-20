-- test clients

-- plain secret value is "secret"
INSERT INTO oauth2_client(id,name,secret,type,super,
  redirect_uri,registered_by, description, url, registration_date, 
  is_permitted) 
VALUES ("fCBbQkAyYzI4NzUxMg","super confidential client",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL", 1, 
  "https://korap.ids-mannheim.de/confidential/redirect", "system",
  "This is a test super confidential client.", 
  "http://korap.ids-mannheim.de/confidential", CURRENT_TIMESTAMP, 1);

  
-- plain secret value is "secret"
INSERT INTO oauth2_client(id,name,secret,type,super,
  redirect_uri,registered_by, description,url,registration_date, 
  is_permitted) 
VALUES ("9aHsGW6QflV13ixNpez","non super confidential client",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL", 0,
  "https://third.party.com/confidential/redirect", "system",
  "This is a test nonsuper confidential client.",
  "http://third.party.com/confidential", CURRENT_TIMESTAMP,1);

INSERT INTO oauth2_client(id,name,secret,type,super,
  registered_by, description,url, registration_date, 
  is_permitted) 
VALUES ("52atrL0ajex_3_5imd9Mgw","confidential client 2",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL", 0,"system",
  "This is a test nonsuper confidential client.",
  "http://example.client.de", CURRENT_TIMESTAMP, 1);

INSERT INTO oauth2_client(id,name,secret,type,super,
  redirect_uri, registered_by, description, url, registration_date, 
  is_permitted)
VALUES ("8bIDtZnH6NvRkW2Fq","third party client",null,
  "PUBLIC", 0,
  "https://third.party.client.com/redirect","system",
  "This is a test public client.",
  "http://third.party.client.com", CURRENT_TIMESTAMP,1);

  
INSERT INTO oauth2_client(id,name,secret,type,super,
  registered_by, description, url, registration_date, 
  is_permitted) 
VALUES ("nW5qM63Rb2a7KdT9L","test public client",null,
  "PUBLIC", 0, "https://korap.ids-mannheim.de/public/redirect",
  "system", "http://korap.ids-mannheim.de/public", 
  CURRENT_TIMESTAMP, 1);
  

INSERT INTO oauth2_access_token(token,user_id,created_date, 
expiry_date, user_auth_time)
VALUES("fia0123ikBWn931470H8s5gRqx7Moc4p","marlin",1527776750000, 
1527776750000, 1527690190000);

INSERT INTO oauth2_refresh_token(token,user_id,user_auth_time, 
created_date, expiry_date, client)
VALUES("js9iQ4lw1Ri7fz06l0dXl8fCVp3Yn7vmq8","pearl",1496154350000, 
1496240795000, 1527784020000, "nW5qM63Rb2a7KdT9L");

-- EM: expiry date must be in epoch milis format for testing with sqlite,
-- on the contrary, for testing using mysql use this format: "2018-05-31 16:27:00"
-- otherwise criteria query using greaterThan does not work. 
