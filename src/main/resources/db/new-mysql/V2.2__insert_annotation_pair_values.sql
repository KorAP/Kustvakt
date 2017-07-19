INSERT INTO annotation_pair_value (pair_id, value_id) 
	SELECT 
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.code="opennlp") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Structure")), 
		(SELECT a.id FROM annotation as a WHERE a.description="Sentence");
		
INSERT INTO annotation_pair_value (pair_id, value_id) 
	SELECT 
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.code="opennlp") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Structure")), 
		(SELECT a.id FROM annotation as a WHERE a.description="Paragraph");

INSERT INTO annotation_pair_value (pair_id, value_id) 
	SELECT 
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.code="opennlp") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Part of speech")), 
		(SELECT a.id FROM annotation as a WHERE a.description="Attributive Adjective");

INSERT INTO annotation_pair_value (pair_id, value_id) 
	SELECT 
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.code="dereko") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Structure")), 
		(SELECT a.id FROM annotation as a WHERE a.description="Sentence");
