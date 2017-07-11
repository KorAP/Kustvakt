--foundries
INSERT INTO annotation (symbol, type, description) VALUES("base",1,"Base");
INSERT INTO annotation (symbol, type, description) VALUES("dereko",1,"DeReKo");
INSERT INTO annotation (symbol, type, description) VALUES("corenlp",1,"CoreNLP");
INSERT INTO annotation (symbol, type, description) VALUES("cnx",1,"Connexor");
INSERT INTO annotation (symbol, type, description) VALUES("drukola",1,"DruKoLa");
INSERT INTO annotation (symbol, type, description) VALUES("glemm",1,"Glemm");
INSERT INTO annotation (symbol, type, description) VALUES("malt",1,"Malt");
INSERT INTO annotation (symbol, type, description) VALUES("marmot",1,"MarMot");
INSERT INTO annotation (symbol, type, description) VALUES("mate",1,"Mate");
INSERT INTO annotation (symbol, type, description) VALUES("mdp",1,"MD parser");
INSERT INTO annotation (symbol, type, description) VALUES("opennlp",1,"OpenNLP");
INSERT INTO annotation (symbol, type, description) VALUES("sgbr",1,"Schreibgebrauch");
INSERT INTO annotation (symbol, type, description) VALUES("tt",1,"Tree Tagger");
INSERT INTO annotation (symbol, type, description) VALUES("xip",1,"Xerox Incremental Parser");

--layers
INSERT INTO annotation (symbol, type, description) VALUES("c",2,"Constituency");
INSERT INTO annotation (symbol, type, description) VALUES("d",2,"Dependency");
INSERT INTO annotation (symbol, type, description) VALUES("p",2,"Part of speech");
INSERT INTO annotation (symbol, type, description) VALUES("l",2,"Lemma");
INSERT INTO annotation (symbol, type, description) VALUES("lv",2,"Lemma variant");
INSERT INTO annotation (symbol, type, description) VALUES("m",2,"Morphology");
INSERT INTO annotation (symbol, type, description) VALUES("ne",2,"Named entity");
INSERT INTO annotation (symbol, type, description) VALUES("s",2,"Structure");
INSERT INTO annotation (symbol, type, description) VALUES("syn",2,"Syntax");

--values
INSERT INTO annotation (symbol, type, description) VALUES("s",0,"Sentence");
INSERT INTO annotation (symbol, type, description) VALUES("p",0,"Paragraph");
INSERT INTO annotation (symbol, type, description) VALUES("t",0,"Text");

