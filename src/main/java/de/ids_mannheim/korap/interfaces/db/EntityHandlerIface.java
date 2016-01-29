package de.ids_mannheim.korap.interfaces.db;

import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;

/**
 * User: hanl
 * Date: 8/19/13
 * Time: 11:04 AM
 */
public interface EntityHandlerIface {

    User getAccount(String username)
            throws EmptyResultException, KustvaktException;

    int updateAccount(User user) throws KustvaktException;

    int createAccount(User user) throws KustvaktException;

    int deleteAccount(Integer userid) throws KustvaktException;

    int resetPassphrase(String username, String uriToken, String passphrase)
            throws KustvaktException;

    int activateAccount(String username, String uriToken)
            throws KustvaktException;

}
