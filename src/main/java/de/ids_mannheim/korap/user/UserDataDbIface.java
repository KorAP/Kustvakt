package de.ids_mannheim.korap.user;

import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * @author hanl
 * @date 27/01/2016
 */
public interface UserDataDbIface<T extends Userdata> {

    public int store(T data) throws KustvaktException;

    public int update(T data) throws KustvaktException;

    public T get(Integer id) throws KustvaktException;

    public T get(User user) throws KustvaktException;

    public int delete(T data) throws KustvaktException;

    public int deleteAll() throws KustvaktException;

}
