package de.ids_mannheim.korap.misc;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.service.FastJerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by hanl on 17.04.16.
 */
public class PublicAPITest extends FastJerseyTest {


    @BeforeClass
    public static void setup () {
        FastJerseyTest.setPackages("de.ids_mannheim.korap.web.service.light",
                "de.ids_mannheim.korap.web.filter",
                "de.ids_mannheim.korap.web.utils");
    }


    @Override
    public void initMethod () throws KustvaktException {}


    @Test
    public void testContextSpanSent () {

    }


    @Test
    public void testContextSpanPara () {

    }


    @Test
    public void testSimpleSearch () {

    }


}
