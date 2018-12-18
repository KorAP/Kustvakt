package de.ids_mannheim.korap.resource.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author hanl
 * @date 18/06/2015
 */
//todo: check position and information of rewrites!
public class FoundryRewriteTest extends SpringJerseyTest {

    private static String simple_add_query = "[pos=ADJA]";
    private static String simple_rewrite_query = "[base=Haus]";
    private static String complex_rewrite_query = "<c=INFC>";
    private static String complex_rewrite_query2 = "[orth=laufe/i & base!=Lauf]";
    private static String complex_rewrite_query3 = "[(base=laufen | base=gehen) & tt/pos=VVFIN]";

    @Autowired
    public KustvaktConfiguration config;
    @Autowired
    public RewriteHandler handler; 
    
    @Test
    public void testDefaultLayerMapperThrowsNoException () {
        LayerMapper m = new LayerMapper(config);

        assertEquals(config.getDefault_lemma(), m.findFoundry("lemma"));
        assertEquals(config.getDefault_pos(), m.findFoundry("pos"));
        assertEquals(config.getDefault_token(), m.findFoundry("surface"));
        assertEquals(config.getDefault_dep(), m.findFoundry("d"));
        assertEquals(config.getDefault_const(), m.findFoundry("c"));
    }


    @Test
    public void testDefaultFoundryInjectLemmaThrowsNoError ()
            throws KustvaktException {

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode node = JsonUtils.readTree(result);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(config.getDefault_lemma(), node.at("/query/wrap/foundry")
                .asText());
        assertEquals("lemma", node.at("/query/wrap/layer").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testDefaultFoundryInjectPOSNoErrors () throws KustvaktException {

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(config.getDefault_pos(), node.at("/query/wrap/foundry")
                .asText());
        assertEquals("pos", node.at("/query/wrap/layer").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());

    }

    // EM: Fix me usersetting
//    @Test
//    @Ignore
//    public void testRewriteFoundryInjectPOSThrowsNoError ()
//            throws KustvaktException {
//        User user = helper().getUser();
//
//        RewriteHandler handler = new RewriteHandler();
//        handler.insertBeans(helper().getContext());
//        handler.add(FoundryInject.class);
//        QuerySerializer s = new QuerySerializer();
//        s.setQuery("[pos=ADJA]", "poliqarp");
//        String result = handler.processQuery(s.toJSON(), user);
//        JsonNode node = JsonUtils.readTree(result);
//
//        UserDataDbIface dao = BeansFactory.getTypeFactory()
//                .getTypeInterfaceBean(
//                        helper().getContext().getUserDataProviders(),
//                        UserSettings.class);
//        UserSettings settings = (UserSettings) dao.get(user);
//        assertTrue(settings.isValid());
//        String pos = (String) settings.get(Attributes.DEFAULT_POS_FOUNDRY);
//
//        assertNotNull(node);
//        assertEquals("pos", node.at("/query/wrap/layer").asText());
//        assertEquals(pos, node.at("/query/wrap/foundry").asText());
//        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
//        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
//                .asText());
//    }

    // EM: Fix me usersetting
//    @Test
//    @Ignore
//    public void testRewriteFoundryInjectLemmaThrowsNoError ()
//            throws KustvaktException {
//        KustvaktConfiguration c = helper().getBean(
//                ContextHolder.KUSTVAKT_CONFIG);
//        User user = helper().getUser();
//
//        RewriteHandler handler = new RewriteHandler();
//        handler.insertBeans(helper().getContext());
//        handler.add(FoundryInject.class);
//        QuerySerializer s = new QuerySerializer();
//        s.setQuery("[base=Haus]", "poliqarp");
//        String result = handler.processQuery(s.toJSON(), user);
//        JsonNode node = JsonUtils.readTree(result);
//
//        UserDataDbIface dao = BeansFactory.getTypeFactory()
//                .getTypeInterfaceBean(
//                        helper().getContext().getUserDataProviders(),
//                        UserSettings.class);
//        UserSettings settings = (UserSettings) dao.get(user);
//        assertTrue(settings.isValid());
//        String lemma = (String) settings.get(Attributes.DEFAULT_LEMMA_FOUNDRY);
//
//        assertNotNull(node);
//        assertEquals("lemma", node.at("/query/wrap/layer").asText());
//        assertEquals(lemma, node.at("/query/wrap/foundry").asText());
//        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
//        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
//                .asText());
//    }


    @Test
    public void testFoundryInjectJoinedQueryNoErrors ()
            throws KustvaktException {

        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=laufe/i & base!=Lauf]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertEquals("koral:termGroup", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/operands/0/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/rewrites").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }


    @Test
    public void testFoundryInjectGroupedQueryNoErrors ()
            throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[(base=laufen | tt/pos=VVFIN)]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode node = JsonUtils.readTree(result);
//        System.out.println("NODDE "+ node);
        assertNotNull(node);
        assertEquals("koral:termGroup", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/operands/0/foundry")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/rewrites")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertTrue(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }

    @Test
    public void testFoundryBaseRewrite() throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=laufen]", "poliqarp");
        String result = handler.processQuery(s.toJSON(), new KorAPUser());
        JsonNode node = JsonUtils.readTree(result);
        System.out.println("NODE "+ node);
//        assertNotNull(node);
        assertEquals("koral:term", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/foundry")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/rewrites")
                .isMissingNode());
    }

}
