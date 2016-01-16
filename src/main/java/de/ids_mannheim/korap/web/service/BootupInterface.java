package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * @author hanl
 * @date 12/01/2016
 */
public interface BootupInterface {

    void load() throws KustvaktException;
    int position();


}
