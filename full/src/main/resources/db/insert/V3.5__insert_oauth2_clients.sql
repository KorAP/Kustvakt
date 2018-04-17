-- test clients

-- plain secret value is "secret"
INSERT INTO oauth2_client(id,name,secret,type,native, url,url_hashcode,
  redirect_uri,registered_by) 
VALUES ("fCBbQkAyYzI4NzUxMg==","test confidential client",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL", 1, "http://korap.ids-mannheim.de/confidential", 2087150261, 
  "https://korap.ids-mannheim.de/confidential/redirect", "system");
  
INSERT INTO oauth2_client(id,name,secret,type,url,url_hashcode,
  redirect_uri, registered_by) 
VALUES ("8bIDtZnH6NvRkW2Fq==","third party client",null,
  "PUBLIC","http://third.party.client.com", -2137275617,
  "https://third.party.client.com/redirect","system");
  
INSERT INTO oauth2_client(id,name,secret,type,native,url,url_hashcode,
  redirect_uri, registered_by) 
VALUES ("iBr3LsTCxOj7D2o0A5m","test public client",null,
  "PUBLIC", 1, "http://korap.ids-mannheim.de/public", 1360724310,
  "https://korap.ids-mannheim.de/public/redirect","system"); 
  