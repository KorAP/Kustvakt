package de.ids_mannheim.korap.dao;

import org.springframework.stereotype.Repository;

import de.ids_mannheim.korap.user.KorAPUser;
import de.ids_mannheim.korap.user.User;

/** Dummy DAO for testing using basic authentication.
 * 
 * @author margaretha
 *
 */
@Repository
public class UserDao {

    public User getAccount (String username) {
        User user = new KorAPUser();
        user.setUsername(username);
        return user;
    }


}
