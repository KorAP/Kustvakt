package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.EmptyResultException;
import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.UserDetails;
import de.ids_mannheim.korap.user.UserSettings;

/**
 * User: hanl
 * Date: 8/19/13
 * Time: 11:04 AM
 */
public interface EntityHandlerIface {
    UserSettings getUserSettings(Integer userid) throws KorAPException;

    int updateSettings(UserSettings settings) throws KorAPException;

    UserDetails getUserDetails(Integer userid) throws KorAPException;

    int updateUserDetails(UserDetails details) throws KorAPException;

    //    List<UserQuery> getUserQueries(User user) throws KorAPException;

    //    UserQuery getUserQuery(String id) throws KorAPException;

    //    void updateUserQueries(User user, List<UserQuery> newOnes) throws KorAPException;

    User getAccount(String username) throws
            EmptyResultException, KorAPException;

    int updateAccount(User user) throws KorAPException;

    int createAccount(User user) throws KorAPException;

    int deleteAccount(Integer userid) throws KorAPException;

    int resetPassphrase(String username, String uriToken,
            String passphrase) throws KorAPException;

    int activateAccount(String username, String uriToken)
            throws KorAPException;

}
