package de.ids_mannheim.korap.interfaces.db;

import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserSettings;

/**
 * User: hanl
 * Date: 8/19/13
 * Time: 11:04 AM
 */
public interface EntityHandlerIface {
    @Deprecated
    UserSettings getUserSettings(Integer userid) throws KustvaktException;

    @Deprecated
    int updateSettings(UserSettings settings) throws KustvaktException;

    @Deprecated
    UserDetails getUserDetails(Integer userid) throws KustvaktException;

    @Deprecated
    int updateUserDetails(UserDetails details) throws KustvaktException;

    //    List<UserQuery> getUserQueries(User user) throws KorAPException;

    //    UserQuery getUserQuery(String id) throws KorAPException;

    //    void updateUserQueries(User user, List<UserQuery> newOnes) throws KorAPException;

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
