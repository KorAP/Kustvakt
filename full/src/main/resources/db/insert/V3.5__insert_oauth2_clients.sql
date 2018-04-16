-- test clients

-- plain secret value is "secret"
INSERT INTO oauth2_client(id,name,secret,type,url,url_hashcode,redirect_uri,
  registered_by) 
VALUES ("fCBbQkAyYzI4NzUxMg==","test confidential client",
  "$2a$08$vi1FbuN3p6GcI1tSxMAoeuIYL8Yw3j6A8wJthaN8ZboVnrQaTwLPq",
  "CONFIDENTIAL","http://confidential.client.com", -1097645390, 
  "https://confidential.client.com/redirect", "system");
  
INSERT INTO oauth2_client(id,name,secret,type,url,url_hashcode,redirect_uri,
  registered_by) 
VALUES ("8bIDtZnH6NvRkW2Fq==","test public client",null,
  "PUBLIC","http://public.client.com", -1408041551,
  "https://public.client.com/redirect","system");