package de.ids_mannheim.korap.dao;

import de.ids_mannheim.korap.user.User;

public class DummyAdminDaoImpl implements AdminDao {

    @Override
    public void addAccount (User user) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAdmin (String userId) {
        return false;
    }

}
