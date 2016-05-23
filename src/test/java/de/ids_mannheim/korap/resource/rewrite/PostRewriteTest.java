package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.utils.JsonUtils;
import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 12/11/2015
 */
//fixme: tests only work with singleconnection data sources
// todo: logging!
public class PostRewriteTest extends BeanConfigTest {

    @Override
    public void initMethod () throws KustvaktException {

    }


    // otherwise cache will maintain values not relevant for other tests
    @Before
    public void before () {
        CacheManager.getInstance().getCache("documents").removeAll();
        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());
        dao.truncate();
    }


    @Test
    public void testPostRewriteNothingToDo () throws KustvaktException {
        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewriter instance!", true,
                ha.add(DocMatchRewrite.class));

        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());
        try {
            Document d = dao.findbyId("BRZ13_APR.00014", null);
            assertNull(d);
            String v = ha.postProcess(RESULT, null);
            assertEquals("results do not match", JsonUtils.readTree(RESULT),
                    JsonUtils.readTree(v));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testPostRewriteRemoveDoc () {
        DocumentDao dao = new DocumentDao(helper().getContext()
                .getPersistenceClient());

        Document doc = new Document("BRZ13_APR.00014");
        doc.setDisabled(true);
        try {
            dao.storeResource(doc, null);
        }
        catch (KustvaktException e) {
            return;
        }

        RewriteHandler ha = new RewriteHandler();
        ha.insertBeans(helper().getContext());
        assertEquals("Handler could not be added to rewriter instance!", true,
                ha.add(DocMatchRewrite.class));

        String v = ha.postProcess(RESULT, null);

        JsonNode node = JsonUtils.readTree(v);

        assertNotEquals("Wrong DocID", "BRZ13_APR.00014",
                node.at("/matches/1/docID"));

        try {
            dao.deleteResource(doc.getPersistentID(), null);
            Document d = dao.findbyId(doc.getPersistentID(), null);
            if (d != null)
                System.out.println("IS SUPPOSED TO BE NULL! " + d);
        }
        catch (KustvaktException e) {
            e.printStackTrace();
            return;
        }

    }


    @Test
    public void testPath () {
        String v = "{\n" + "    \"meta\": {\n" + "        \"count\": 25,\n"
                + "        \"startIndex\": 0,\n"
                + "        \"timeout\": 120000,\n" + "        \"context\": {\n"
                + "            \"left\": [\n" + "                \"token\",\n"
                + "                6\n" + "            ],\n"
                + "            \"right\": [\n" + "                \"token\",\n"
                + "                6\n" + "            ]\n" + "        }}}";
        JsonNode n = JsonUtils.readTree(v);

    }

    private static final String RESULT = "{\n"
            + "    \"meta\": {\n"
            + "        \"count\": 25,\n"
            + "        \"startIndex\": 0,\n"
            + "        \"timeout\": 120000,\n"
            + "        \"context\": {\n"
            + "            \"left\": [\n"
            + "                \"token\",\n"
            + "                6\n"
            + "            ],\n"
            + "            \"right\": [\n"
            + "                \"token\",\n"
            + "                6\n"
            + "            ]\n"
            + "        },\n"
            + "        \"fields\": [\n"
            + "            \"textSigle\",\n"
            + "            \"author\",\n"
            + "            \"docSigle\",\n"
            + "            \"title\",\n"
            + "            \"pubDate\",\n"
            + "            \"UID\",\n"
            + "            \"corpusID\",\n"
            + "            \"textClass\",\n"
            + "            \"subTitle\",\n"
            + "            \"layerInfos\",\n"
            + "            \"ID\",\n"
            + "            \"pubPlace\",\n"
            + "            \"corpusSigle\"\n"
            + "        ],\n"
            + "        \"version\": \"unknown\",\n"
            + "        \"benchmark\": \"0.204314141 s\",\n"
            + "        \"totalResults\": 1755,\n"
            + "        \"serialQuery\": \"tokens:tt/l:Wort\",\n"
            + "        \"itemsPerPage\": 25\n"
            + "    },\n"
            + "    \"query\": {\n"
            + "        \"@type\": \"koral:token\",\n"
            + "        \"wrap\": {\n"
            + "            \"@type\": \"koral:term\",\n"
            + "            \"key\": \"Wort\",\n"
            + "            \"layer\": \"lemma\",\n"
            + "            \"match\": \"match:eq\",\n"
            + "            \"foundry\": \"tt\",\n"
            + "            \"rewrites\": [\n"
            + "                {\n"
            + "                    \"@type\": \"koral:rewrite\",\n"
            + "                    \"src\": \"Kustvakt\",\n"
            + "                    \"operation\": \"operation:injection\"\n"
            + "                }\n"
            + "            ]\n"
            + "        }\n"
            + "    },\n"
            + "    \"matches\": [\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"staat-gesellschaft familie-geschlecht\",\n"
            + "            \"title\": \"Sexueller Missbrauch –„Das schreiende Kind steckt noch tief in mir“\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>in ihrer Stimme schwingt bei diesem </span><mark>Wort</mark><span class=\\\"context-right\\\"> Sarkasmus mit. Bis man einen passenden<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00001-p454-455\",\n"
            + "            \"docID\": \"BRZ13_APR.00001\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"freizeit-unterhaltung reisen\",\n"
            + "            \"title\": \"Leben dick und prall\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>der DLRG, ausgelassene Partys und markige </span><mark>Worte</mark><span class=\\\"context-right\\\"> des Dompredigers: „Ostern ist kein goethischer<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00014-p96-97\",\n"
            + "            \"docID\": \"BRZ13_APR.00014\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"staat-gesellschaft biographien-interviews kultur musik\",\n"
            + "            \"title\": \"So wird es gemacht:\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>derfehlende Buchstabe.Gelingt es dir,das </span><mark>Wort</mark><span class=\\\"context-right\\\"> vervollständigen? Tipp: Probiere auch mal rückwärts<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00055-p19-20\",\n"
            + "            \"docID\": \"BRZ13_APR.00055\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik ausland\",\n"
            + "            \"title\": \"Südkorea droht mit Angriffen – USA rüsten auf\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>der Stunde. „Aus einem Krieg der </span><mark>Worte</mark><span class=\\\"context-right\\\"> darf kein echter Krieg werden“, sagte<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00076-p238-239\",\n"
            + "            \"docID\": \"BRZ13_APR.00076\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland freizeit-unterhaltung reisen\",\n"
            + "            \"title\": \"Dauercamper kämpfen für ihren Platz\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>„Initiative Campingplatz Räbke“. „Als ich das </span><mark>Wort</mark><span class=\\\"context-right\\\"> Schließung gelesen habe, war ich richtig<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00200-p255-256\",\n"
            + "            \"docID\": \"BRZ13_APR.00200\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"freizeit-unterhaltung reisen\",\n"
            + "            \"title\": \"Neue Aktionen lockten Besucher\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>Jan Waldheim (CWG) unter den aufmunternden </span><mark>Worten</mark><span class=\\\"context-right\\\"> eines augenzwinkernden Axel Schnalke („Ein bisschen<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00210-p142-143\",\n"
            + "            \"docID\": \"BRZ13_APR.00210\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"kultur musik\",\n"
            + "            \"title\": \"Travestie – Helden in Strumpfhosen\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>trotzdem nicht. Selten wurden so viele </span><mark>Worte</mark><span class=\\\"context-right\\\">, die der Autor hier lieber verschweigt<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00253-p166-167\",\n"
            + "            \"docID\": \"BRZ13_APR.00253\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"kultur musik\",\n"
            + "            \"title\": \"Travestie – Helden in Strumpfhosen\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>zudem nicht immer nur bei den </span><mark>Worten</mark><span class=\\\"context-right\\\"> geblieben) und dabei gleichzeitig soviel Charme<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00253-p191-192\",\n"
            + "            \"docID\": \"BRZ13_APR.00253\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"kultur musik\",\n"
            + "            \"title\": \"Travestie – Helden in Strumpfhosen\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>bedeutet Travestie sich zu verkleiden, das </span><mark>Wort</mark><span class=\\\"context-right\\\"> stammt aus dem Französischen. Traditionell belegten<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00253-p371-372\",\n"
            + "            \"docID\": \"BRZ13_APR.00253\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"sport fussball\",\n"
            + "            \"title\": \"VfL kommt nicht vom Fleck\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>am internationalen Geschäft berechtigt. Mit anderen </span><mark>Worten</mark><span class=\\\"context-right\\\">: Die „Wölfe“ stecken im grauen Mittelmaß<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00260-p93-94\",\n"
            + "            \"docID\": \"BRZ13_APR.00260\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"sport fussball\",\n"
            + "            \"title\": \"Mensch, Mayer! Super Tor\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>bekommst, ist das unbezahlbar – ein Bonus.“ </span><mark>Worte</mark><span class=\\\"context-right\\\">, die dem Torschützen weiteres Selbstvertrauen geben<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00275-p271-272\",\n"
            + "            \"docID\": \"BRZ13_APR.00275\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"sport fussball\",\n"
            + "            \"title\": \"Nur Gerücht? KHL-Klub will „Dshuni“\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>und hakt das Thema ab.cb Kein </span><mark>Wort</mark><span class=\\\"context-right\\\"> zum Interesse aus Astana: Daniar Dshunussow.Foto<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00277-p163-164\",\n"
            + "            \"docID\": \"BRZ13_APR.00277\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-02\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"staat-gesellschaft biographien-interviews\",\n"
            + "            \"title\": \"Das Leben ist nicht auf diese Erde beschränkt\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>1. Korinther 1,18 denken: Denn das </span><mark>Wort</mark><span class=\\\"context-right\\\"> vom Kreuz ist eine Torheit denen<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00520-p32-33\",\n"
            + "            \"docID\": \"BRZ13_APR.00520\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-03\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"sport fussball\",\n"
            + "            \"title\": \"Allofs und Hecking knöpfensich die VfL-Profis vor\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>für die Profis am Dienstag klare </span><mark>Worte</mark><span class=\\\"context-right\\\"> vom Führungsduo. Von Thorsten Grunow Wolfsburg<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00557-p17-18\",\n"
            + "            \"docID\": \"BRZ13_APR.00557\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-03\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"sport fussball\",\n"
            + "            \"title\": \"Allofs und Hecking knöpfensich die VfL-Profis vor\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>Minuten setzte es am Dienstagnachmittag klare </span><mark>Worte</mark><span class=\\\"context-right\\\"> für die kickende Belegschaft, die durchaus<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00557-p60-61\",\n"
            + "            \"docID\": \"BRZ13_APR.00557\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-03\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"sport fussball\",\n"
            + "            \"title\": \"Allofs und Hecking knöpfensich die VfL-Profis vor\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>Manager ist überzeugt, dass die klaren </span><mark>Worte</mark><span class=\\\"context-right\\\"> auf fruchtbaren Boden gefallen sind. „Ich<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_APR.00557-p235-236\",\n"
            + "            \"docID\": \"BRZ13_APR.00557\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-04-03\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland politik ausland\",\n"
            + "            \"title\": \"Zeitungsartikelzufällig deponiert?\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>erspart“, lautete die Schlagzeile – wobei das </span><mark>Wort</mark><span class=\\\"context-right\\\"> „erspart“ abgeschnitten war. Ein plumper Versuch<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07616-p31-32\",\n"
            + "            \"docID\": \"BRZ13_JAN.07616\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland\",\n"
            + "            \"title\": \"„Philipp Rösler wackelt nicht“\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>Vizekanzler bei all dem Jubel zu </span><mark>Wort</mark><span class=\\\"context-right\\\"> kommt. „Ein großartiger Tag“, sagt er<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07617-p177-178\",\n"
            + "            \"docID\": \"BRZ13_JAN.07617\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"freizeit-unterhaltung reisen\",\n"
            + "            \"title\": \"Lanz gibt den charmanten, zurückhaltenden Gastgeber\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>er, als seine Assistentin ihm ins </span><mark>Wort</mark><span class=\\\"context-right\\\"> fiel. Dennoch holte das ungleiche Duo<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07621-p261-262\",\n"
            + "            \"docID\": \"BRZ13_JAN.07621\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland\",\n"
            + "            \"title\": \"Mundlos denkt über Rücktritt nach\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>räumte selbst ein, wie sarkastisch diese </span><mark>Worte</mark><span class=\\\"context-right\\\"> nach einer solchen Wahlnacht klingen mussten<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07694-p192-193\",\n"
            + "            \"docID\": \"BRZ13_JAN.07694\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland\",\n"
            + "            \"title\": \"BraunschweigGold – Hannover Blech\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>im Volksfreundhaus hört man kein schlechtes </span><mark>Wort</mark><span class=\\\"context-right\\\"> über den Kanzlerkandidaten Peer Steinbrück – und<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07695-p169-170\",\n"
            + "            \"docID\": \"BRZ13_JAN.07695\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland\",\n"
            + "            \"title\": \"BraunschweigGold – Hannover Blech\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>Volksfreundhaus merklich steigen. Hier hat das </span><mark>Wort</mark><span class=\\\"context-right\\\"> von der Wahlparty bei Bier, Bockwurst<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07695-p266-267\",\n"
            + "            \"docID\": \"BRZ13_JAN.07695\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"staat-gesellschaft kirche\",\n"
            + "            \"title\": \"Fernsehen überträgt Gottesdienst\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>schon mal von der Sendung „Das </span><mark>Wort</mark><span class=\\\"context-right\\\"> zum Sonntag“ gehört. Das sind Predigten<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07840-p37-38\",\n"
            + "            \"docID\": \"BRZ13_JAN.07840\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland\",\n"
            + "            \"title\": \"Wahlkrimi im Ratssaal\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\"><span class=\\\"more\\\"></span>ihre Konkurrentin Glosemeyer hatte sie warme </span><mark>Worte</mark><span class=\\\"context-right\\\"> übrig. „Für den ersten Anlauf eine<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07848-p147-148\",\n"
            + "            \"docID\": \"BRZ13_JAN.07848\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"field\": \"tokens\",\n"
            + "            \"textClass\": \"politik inland\",\n"
            + "            \"title\": \"Warme Worte nach eiskaltem Wahlkampf\",\n"
            + "            \"author\": \"\",\n"
            + "            \"startMore\": true,\n"
            + "            \"endMore\": true,\n"
            + "            \"corpusID\": \"BRZ13\",\n"
            + "            \"snippet\": \"<span class=\\\"context-left\\\">Warme </span><mark>Worte</mark><span class=\\\"context-right\\\"> nach eiskaltem Wahlkampf Die SPD feierte<span class=\\\"more\\\"></span></span>\",\n"
            + "            \"matchID\": \"match-BRZ13!BRZ13_JAN.07850-p1-2\",\n"
            + "            \"docID\": \"BRZ13_JAN.07850\",\n"
            + "            \"UID\": 0,\n"
            + "            \"pubDate\": \"2013-01-21\"\n" + "        }\n"
            + "    ]\n" + "}";

}
