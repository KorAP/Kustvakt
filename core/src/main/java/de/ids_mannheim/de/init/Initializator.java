package de.ids_mannheim.de.init;

import java.io.IOException;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.util.QueryException;

/** Init methods run after spring dependency injection 
 *  
 * @author margaretha
 *
 */
public interface Initializator {

    void init () throws IOException, QueryException, KustvaktException;

    void initTest () throws IOException, KustvaktException;

}