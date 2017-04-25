package de.ids_mannheim.korap.interfaces.db;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;

/**
 * @author margaretha
 */
public interface AdminHandlerIface {

    int updateAccount (User user) throws KustvaktException;


    int addAccount (User user) throws KustvaktException;


    int deleteAccount (Integer userid) throws KustvaktException;


    int truncate () throws KustvaktException;

	boolean isAdmin(int userId);

}
