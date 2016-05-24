package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.db.UserDataDbIface;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.user.UserSettings;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 21/10/2015
 */
public class RewriteHandlerTest extends BeanConfigTest {

    @Test
    public void initHandler () {
        try {
            RewriteHandler handler = new RewriteHandler();
            handler.insertBeans(helper().getContext());
            assertTrue(handler.add(FoundryInject.class));
        }
        catch (Exception e) {
            e.printStackTrace();

        }
    }


    @Test
    public void testRewriteTaskAdd () {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        assertTrue(handler.add(FoundryInject.class));
        assertTrue(handler.add(DocMatchRewrite.class));
        assertTrue(handler.add(CollectionCleanupFilter.class));
        assertTrue(handler.add(IdWriter.class));
    }


    // throws exception cause of missing configuration
    @Test(expected = RuntimeException.class)
    public void testRewriteConfigThrowsException () {
        RewriteHandler handler = new RewriteHandler();
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[(base=laufen | base=gehen) & tt/pos=VVFIN]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        handler.preProcess(s.toJSON(), null);
    }


    @Test
    public void testRewriteNoBeanInject () {
        RewriteHandler handler = new RewriteHandler(helper().getContext()
                .getConfiguration());
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[(base=laufen | base=gehen) & tt/pos=VVFIN]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        String res = handler.preProcess(s.toJSON(), null);
        assertNotNull(res);
    }


    @Test
    public void testRewriteBeanInject () {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[(base=laufen | base=gehen) & tt/pos=VVFIN]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        String res = handler.preProcess(s.toJSON(), null);
        JsonNode node = JsonUtils.readTree(res);
        assertNotNull(node);

        assertEquals("tt", node.at("/query/wrap/operands/0/operands/0/foundry")
                .asText());
        assertEquals("tt", node.at("/query/wrap/operands/0/operands/1/foundry")
                .asText());
        assertEquals("tt", node.at("/query/wrap/operands/1/foundry").asText());
    }


    @Test
    public void testRewriteUserSpecific () {
        RewriteHandler handler = new RewriteHandler();
        handler.insertBeans(helper().getContext());
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[(base=laufen | base=gehen) & tt/pos=VVFIN]", "poliqarp");
        assertTrue(handler.add(FoundryInject.class));
        String res = handler.preProcess(s.toJSON(), helper().getUser());
        JsonNode node = JsonUtils.readTree(res);
        assertNotNull(node);
        assertEquals("tt_test",
                node.at("/query/wrap/operands/0/operands/0/foundry").asText());
        assertEquals("tt_test",
                node.at("/query/wrap/operands/0/operands/1/foundry").asText());
        assertNotEquals("tt_test", node.at("/query/wrap/operands/1/foundry")
                .asText());
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
        UserDataDbIface settingsdao = BeansFactory.getTypeFactory()
                .getTypedBean(helper().getContext().getUserDataDaos(),
                        UserSettings.class);
        UserSettings s = (UserSettings) settingsdao.get(helper().getUser());
        s.setField(Attributes.DEFAULT_LEMMA_FOUNDRY, "tt_test");
        settingsdao.update(s);
    }



}
