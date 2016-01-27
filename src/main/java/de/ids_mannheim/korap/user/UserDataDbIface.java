package de.ids_mannheim.korap.user;

/**
 * @author hanl
 * @date 27/01/2016
 */
public interface UserDataDbIface<T extends Userdata> {

    public int store(T data);

    public int update(T data);

    public T get(Integer id);

    public T get(User user);

    public int delete(T data);

    public int deleteAll();

}
