package de.ids_mannheim.korap.handlers;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author hanl
 * @date 12/11/2015
 */
public class DocumentDaoTest extends BeanConfigTest {

    private static DocumentDao dao;


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
    public void testGet () {
        Document doc = new Document("BRZ13_APR.00002");
        doc.setDisabled(true);
        try {
            dao.storeResource(doc, null);
            Document doc1 = dao.findbyId(doc.getPersistentID(), null);
            assert doc1 != null && doc.isDisabled();
        }
        catch (KustvaktException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testRemove () {
        Document doc = new Document("BRZ13_APR.00003");
        doc.setDisabled(true);
        try {
            dao.storeResource(doc, null);
            Document doc1 = dao.findbyId(doc.getPersistentID(), null);
            assert dao.deleteResource(doc.getPersistentID(), null) == 1;
            doc1 = dao.findbyId(doc.getPersistentID(), null);
            assert doc1 == null;
        }
        catch (KustvaktException e) {
            e.printStackTrace();

        }
    }


    @Test
    public void testEmptyFind () {
        List<String> dc = null;
        try {
            dc = dao.findbyCorpus("WPD", true);
        }
        catch (KustvaktException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(dc);
        Assert.assertEquals("should be empty", 0, dc.size());
    }


    @Test
    public void testFind () {
        int length = 10;
        for (int i = 0; i < length; i++) {
            Document doc = new Document("WPD_APR.0000" + i);
            doc.setDisabled(true);
            try {
                dao.storeResource(doc, null);
            }
            catch (KustvaktException e) {
                e.printStackTrace();
                break;
            }
        }

        List<String> dc = null;
        try {
            dc = dao.findbyCorpus("WPD", true);
        }
        catch (KustvaktException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(dc);
        Assert.assertNotSame("should not be empty", 0, dc.size());
        Assert.assertEquals("not all found", length, dc.size());
    }


    @Override
    public void initMethod () throws KustvaktException {
        dao = new DocumentDao(helper().getContext().getPersistenceClient());
    }
}
