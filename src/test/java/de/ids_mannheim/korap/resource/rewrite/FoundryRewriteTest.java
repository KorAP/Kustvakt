package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.resource.LayerMapper;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserSettings;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 18/06/2015
 */
//todo: check position and information of rewrites!
public class FoundryRewriteTest extends BeanConfigTest {

    private static String simple_add_query = "[pos=ADJA]";
    private static String simple_rewrite_query = "[base=Haus]";
    private static String complex_rewrite_query = "<c=INFC>";
    private static String complex_rewrite_query2 = "[orth=laufe/i & base!=Lauf]";
    private static String complex_rewrite_query3 = "[(base=laufen | base=gehen) & tt/pos=VVFIN]";

    private static KustvaktConfiguration config;


    @Override
    public void initMethod () throws KustvaktException {
        config = helper().getContext().getConfiguration();
        helper().setupAccount();
    }


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
    public void testDefaultFoundryInjectLemmaThrowsNoError () {

        KustvaktConfiguration c = helper().getBean(
                ContextHolder.KUSTVAKT_CONFIG);

        RewriteHandler processor = new RewriteHandler();
        processor.insertBeans(helper().getContext());
        processor.add(FoundryInject.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = processor.process(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        System.out.println("REWRITTEN "+ node);
        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(c.getDefault_lemma(), node.at("/query/wrap/foundry")
                .asText());
        assertEquals("lemma", node.at("/query/wrap/layer").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testDefaultFoundryInjectPOSNoErrors () {

        KustvaktConfiguration c = helper().getBean(
                ContextHolder.KUSTVAKT_CONFIG);

        QuerySerializer s = new QuerySerializer();
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        s.setQuery("[pos=ADJA]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        String result = handler.process(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertFalse(node.at("/query/wrap/foundry").isMissingNode());
        assertEquals(c.getDefault_pos(), node.at("/query/wrap/foundry")
                .asText());
        assertEquals("pos", node.at("/query/wrap/layer").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());

    }


    @Test
    public void testRewriteFoundryInjectPOSThrowsNoError ()
            throws KustvaktException {
        User user = helper().getUser();

        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(FoundryInject.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[pos=ADJA]", "poliqarp");
        String result = handler.process(s.toJSON(), user);
        JsonNode node = JsonUtils.readTree(result);

        UserDataDbIface dao = BeansFactory.getTypeFactory().getTypedBean(
                helper().getContext().getUserDataDaos(), UserSettings.class);
        UserSettings settings = (UserSettings) dao.get(user);
        assertTrue(settings.isValid());
        String pos = (String) settings.get(Attributes.DEFAULT_POS_FOUNDRY);

        assertNotNull(node);
        assertEquals("pos", node.at("/query/wrap/layer").asText());
        assertEquals(pos, node.at("/query/wrap/foundry").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testRewriteFoundryInjectLemmaThrowsNoError ()
            throws KustvaktException {
        KustvaktConfiguration c = helper().getBean(
                ContextHolder.KUSTVAKT_CONFIG);
        User user = helper().getUser();

        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        handler.add(FoundryInject.class);
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[base=Haus]", "poliqarp");
        String result = handler.process(s.toJSON(), user);
        JsonNode node = JsonUtils.readTree(result);

        UserDataDbIface dao = BeansFactory.getTypeFactory().getTypedBean(
                helper().getContext().getUserDataDaos(), UserSettings.class);
        UserSettings settings = (UserSettings) dao.get(user);
        assertTrue(settings.isValid());
        String lemma = (String) settings.get(Attributes.DEFAULT_LEMMA_FOUNDRY);

        assertNotNull(node);
        assertEquals("lemma", node.at("/query/wrap/layer").asText());
        assertEquals(lemma, node.at("/query/wrap/foundry").asText());
        assertFalse(node.at("/query/wrap/rewrites").isMissingNode());
        assertEquals("koral:rewrite", node.at("/query/wrap/rewrites/0/@type")
                .asText());
    }


    @Test
    public void testFoundryInjectJoinedQueryNoErrors () {
        KustvaktConfiguration c = helper().getBean(
                ContextHolder.KUSTVAKT_CONFIG);

        QuerySerializer s = new QuerySerializer();
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        s.setQuery("[orth=laufe/i & base!=Lauf]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        String result = handler.process(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertEquals("koral:termGroup", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/operands/0/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/rewrites").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertFalse(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }


    @Test
    public void testFoundryInjectGroupedQueryNoErrors () {
        QuerySerializer s = new QuerySerializer();
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        s.setQuery("[(base=laufen | base=gehen) & tt/pos=VVFIN]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        String result = handler.process(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(result);

        assertNotNull(node);
        assertEquals("koral:termGroup", node.at("/query/wrap/@type").asText());
        assertFalse(node.at("/query/wrap/operands/0/operands/0/foundry")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/operands/0/rewrites")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/operands/1/foundry")
                .isMissingNode());
        assertFalse(node.at("/query/wrap/operands/0/operands/1/rewrites")
                .isMissingNode());

        assertFalse(node.at("/query/wrap/operands/1/foundry").isMissingNode());
        assertTrue(node.at("/query/wrap/operands/1/rewrites").isMissingNode());
    }

}
