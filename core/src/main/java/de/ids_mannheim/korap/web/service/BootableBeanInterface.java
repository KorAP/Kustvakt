package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * @author hanl
 * @date 12/01/2016
 */
@Deprecated
public interface BootableBeanInterface {

    void load (ContextHolder beans) throws KustvaktException;


    Class<? extends BootableBeanInterface>[] getDependencies ();

}
