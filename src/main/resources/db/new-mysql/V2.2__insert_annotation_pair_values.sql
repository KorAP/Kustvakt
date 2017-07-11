INSERT INTO annotation_pair_value (pair_id, value) 
	SELECT 
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.symbol="base") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Structure")), 
		(SELECT a.id FROM annotation as a WHERE a.description="Sentence");
		
INSERT INTO annotation_pair_value (pair_id, value) 
	SELECT 
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.symbol="dereko") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Structure")), 
		(SELECT a.id FROM annotation as a WHERE a.description="Sentence");
