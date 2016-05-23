package de.ids_mannheim.korap.interfaces.db;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.User;
import de.ids_mannheim.korap.user.Userdata;

/**
 * @author hanl
 * @date 27/01/2016
 */
public interface UserDataDbIface<T extends Userdata> {

    public int store (T data) throws KustvaktException;


    public int update (T data) throws KustvaktException;


    public T get (Integer id) throws KustvaktException;


    public T get (User user) throws KustvaktException;


    public int delete (T data) throws KustvaktException;


    public int deleteAll () throws KustvaktException;

}
