package de.ids_mannheim.korap.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.config.TestVariables;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.utils.JsonUtils;

public class TimeoutRewriteTest extends SpringJerseyTest {

	@Autowired
	public KustvaktConfiguration config;

	@Test
	public void testNoRewrite () throws KustvaktException {
		RewriteHandler handler = new RewriteHandler(config);
        handler.add(TimeoutRewrite.class);
        
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("count", 25);
        map.put("timeout", 1000);
        QuerySerializer s = new QuerySerializer(API_VERSION_DOUBLE);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setMeta(map);
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        
        node = node.at("/meta"); 
        assertEquals(1000, node.at("/timeout").asInt());
        assertTrue(node.at("/rewrites").isMissingNode());
	}
	
	@Test
	public void testReplaceTimeout () throws KustvaktException {
		RewriteHandler handler = new RewriteHandler(config);
        handler.add(TimeoutRewrite.class);
        
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("count", 25);
        map.put("timeout", 50000);
        QuerySerializer s = new QuerySerializer(API_VERSION_DOUBLE);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setMeta(map);
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        
        node = node.at("/meta"); 
        assertEquals(10000, node.at("/timeout").asInt());
        assertEquals(50000, node.at("/rewrites/0/original").asInt());
	}
	
	@Test
	public void testAddTimeout () throws KustvaktException {
		RewriteHandler handler = new RewriteHandler(config);
        handler.add(TimeoutRewrite.class);
        
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("count", 25);
        QuerySerializer s = new QuerySerializer(API_VERSION_DOUBLE);
        s.setQuery(TestVariables.SIMPLE_ADD_QUERY, "poliqarp");
        s.setMeta(map);
        String result = s.toJSON();
        JsonNode node = JsonUtils.readTree(handler.processQuery(result,
                User.UserFactory.getUser("test_user")));
        
        node = node.at("/meta"); 
        assertEquals(10000, node.at("/timeout").asInt());
	}
}
