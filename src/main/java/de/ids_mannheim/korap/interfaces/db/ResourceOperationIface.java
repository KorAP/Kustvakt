package de.ids_mannheim.korap.interfaces.db;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.user.User;

import java.util.Collection;
import java.util.List;

// todo: for transaction to work this should go into core module!?!
// todo: user instance only required for auditing pointcut operations
public interface ResourceOperationIface<T extends KustvaktResource> {

    Class<T> getType();

    <T extends KustvaktResource> T findbyId(String id, User user)
            throws KustvaktException;

    <T extends KustvaktResource> T findbyId(Integer id, User user)
            throws KustvaktException;

    List<T> getResources(Collection<Object> ids, User user)
            throws KustvaktException;

    int updateResource(T resource, User user) throws KustvaktException;

    int[] updateResources(List<T> resources, User user)
            throws KustvaktException;

    /**
     * store a resource and return the id of the inserted value
     *
     * @param resource
     * @return
     * @throws KustvaktException
     */
    int storeResource(T resource, User user) throws KustvaktException;

    //    public void deleteResource(Integer id, User user) throws KorAPException;
    int deleteResource(String id, User user) throws KustvaktException;

}
