-- test clients

INSERT INTO oauth2_client_url(url,url_hashcode)
VALUES("http://korap.ids-mannheim.de/confidential", 2087150261);

-- plain secret value is "secret"
INSERT INTO oauth2_client(id,name,secret,type,super,url_id,
  redirect_uri,registered_by, description) 
VALUES ("fCBbQkAyYzI4NzUxMg","test confidential client",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL", 1, 2087150261,
  "https://korap.ids-mannheim.de/confidential/redirect", "system",
  "This is a test super confidential client.");

  
INSERT INTO oauth2_client_url(url,url_hashcode)
VALUES("http://third.party.com/confidential", 1712550103);

-- plain secret value is "secret"
INSERT INTO oauth2_client(id,name,secret,type,super,url_id,
  redirect_uri,registered_by, description) 
VALUES ("9aHsGW6QflV13ixNpez","test non super confidential client",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL", 0, 1712550103,
  "https://third.party.com/confidential/redirect", "system",
  "This is a test nonsuper confidential client.");

  
INSERT INTO oauth2_client_url(url,url_hashcode)
VALUES("http://third.party.client.com", -2137275617);

INSERT INTO oauth2_client(id,name,secret,type,super,url_id,
  redirect_uri, registered_by, description) 
VALUES ("8bIDtZnH6NvRkW2Fq","third party client",null,
  "PUBLIC", 0, -2137275617,
  "https://third.party.client.com/redirect","system",
  "This is a test nonsuper public client.");

  
INSERT INTO oauth2_client_url(url,url_hashcode)
VALUES("http://korap.ids-mannheim.de/public", 1360724310); 
  
--INSERT INTO oauth2_client(id,name,secret,type,super,url_id,
--  redirect_uri, registered_by, description) 
--VALUES ("iBr3LsTCxOj7D2o0A5m","test public client",null,
--  "PUBLIC", 1, 1360724310,
--  "https://korap.ids-mannheim.de/public/redirect","system", 
--  "This is a test super public client."); 

INSERT INTO oauth2_access_token(token,user_id,created_date, user_auth_time)
VALUES("fia0123ikBWn931470H8s5gRqx7Moc4p","marlin","2018-05-30 16:25:50",
"2018-05-30 16:23:10");