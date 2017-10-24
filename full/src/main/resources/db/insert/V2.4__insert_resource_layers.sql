INSERT INTO resource_layer (resource_id, layer_id)	
	SELECT 
		(SELECT id FROM resource WHERE id = "WPD15"),
		(SELECT ap.id FROM annotation_pair as ap WHERE 
			ap.annotation1 = (SELECT a.id FROM annotation as a WHERE a.code="opennlp") AND 
			ap.annotation2 = (SELECT a.id FROM annotation as a WHERE a.description="Part of speech"));