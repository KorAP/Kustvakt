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
    public UserSettings getUserSettings(Integer userid) throws KorAPException;

    public int updateSettings(UserSettings settings) throws KorAPException;

    public UserDetails getUserDetails(Integer userid) throws KorAPException;

    public int updateUserDetails(UserDetails details) throws KorAPException;

    //    public List<UserQuery> getUserQueries(User user) throws KorAPException;

    //    public UserQuery getUserQuery(String id) throws KorAPException;

    //    public void updateUserQueries(User user, List<UserQuery> newOnes) throws KorAPException;

    public User getAccount(String username) throws
            EmptyResultException, KorAPException;

    public int updateAccount(User user) throws KorAPException;

    public int createAccount(User user) throws KorAPException;

    public int deleteAccount(Integer userid) throws KorAPException;

    public int resetPassphrase(String username, String uriToken,
            String passphrase) throws KorAPException;

    public int activateAccount(String username, String uriToken)
            throws KorAPException;

}
