package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.resources.VirtualCollection;

/**
 * @author hanl
 * @date 11/02/2016
 */
public class KustvaktResourceTest {


    public void testIDCreation () {
        KustvaktResource resource = new VirtualCollection(1);
        resource.addField("field_1", "data");

    }


    public void testNullableData () {

    }


    public void testWrongDataType () {

    }



}
