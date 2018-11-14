package de.ids_mannheim.korap.dao;

import de.ids_mannheim.korap.user.User;

public interface AdminDao {

    void addAccount (User user);

    boolean isAdmin (String userId);

}