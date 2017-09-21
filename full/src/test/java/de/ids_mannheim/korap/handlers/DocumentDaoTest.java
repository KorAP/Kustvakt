package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.BeanConfigBaseTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author hanl
 * @date 12/11/2015
 */
public class DocumentDaoTest extends BeanConfigTest {

    @Autowired
    private DocumentDao dao;


    @After
    public void clear () {
        dao.truncate();
    }


    @Test
    public void testSet () throws KustvaktException {
        Document doc = new Document("BRZ13_APR.00001");
        doc.setDisabled(true);
        dao.storeResource(doc, null);
    }


    @Test
    public void testGet () throws KustvaktException {
        Document doc = new Document("BRZ13_APR.00002");
        doc.setDisabled(true);
        dao.storeResource(doc, null);
        Document doc1 = dao.findbyId(doc.getPersistentID(), null);
        assertNotNull(doc1);
        assertTrue(doc.isDisabled());
    }


    @Test
    public void testRemove () throws KustvaktException {
        Document doc = new Document("BRZ13_APR.00003");
        doc.setDisabled(true);
        dao.storeResource(doc, null);
        Document doc1 = dao.findbyId(doc.getPersistentID(), null);
        assertEquals(1, dao.deleteResource(doc.getPersistentID(), null));
        doc1 = dao.findbyId(doc.getPersistentID(), null);
        Assert.assertNull(doc1);
    }


    @Test
    public void testEmptyFind () throws KustvaktException {
        List<String> dc = dao.findbyCorpus("WPD", true);
        assertNotNull(dc);
        assertEquals("should be empty", 0, dc.size());
    }


    @Test
    public void testFindText () throws KustvaktException {
        int length = 10;
        for (int i = 0; i < length; i++) {
            Document doc = new Document("WPD_APR.0000" + i);
            doc.setDisabled(true);
            dao.storeResource(doc, null);
        }
        List<String> dc = dao.findbyCorpus("WPD", true);

        assertNotNull(dc);
        assertNotSame("should not be empty", 0, dc.size());
        assertEquals("not all found", length, dc.size());
    }


    @Test
    public void testFindDocByText () throws KustvaktException {
        Document doc = new Document("WPD_AAA", "WPD_AAA.02439");
        doc.setDisabled(true);
        dao.storeResource(doc, null);

        Document dfind = dao.findbyId(doc.getPersistentID(), null);
        assertNotNull(dfind);
        assertEquals(doc.getPersistentID(), dfind.getPersistentID());
    }


    @Test
    public void testFindDocByPartial () throws KustvaktException {
        Document doc = new Document("WPD_AAA.02439");
        doc.setDisabled(true);
        Document doc1 = new Document("WPD_AAA.02343");
        dao.storeResource(doc, null);
        dao.storeResource(doc1, null);

        List<Document> dfind = dao.findbyPartialId(doc.getDocSigle(), null);
        assertNotNull(dfind);
        assertNotEquals(0, dfind.size());
        assertEquals(2, dfind.size());
        assertEquals(doc.getDocSigle(), dfind.get(0).getDocSigle());

        dfind = dao.findbyPartialId(doc.getCorpus(), null);
        assertNotNull(dfind);
        assertNotEquals(0, dfind.size());
        assertEquals(2, dfind.size());
        assertEquals(doc.getDocSigle(), dfind.get(0).getDocSigle());
    }


    @Override
    public void initMethod () throws KustvaktException {
        dao = new DocumentDao(helper().getContext().getPersistenceClient());
    }
}
