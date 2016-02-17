package de.ids_mannheim.korap.interfaces.db;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.resources.KustvaktResource;
import de.ids_mannheim.korap.security.Parameter;
import de.ids_mannheim.korap.security.PolicyCondition;
import de.ids_mannheim.korap.security.SecurityPolicy;
import de.ids_mannheim.korap.user.User;

import java.util.List;

/**
 * User: hanl
 * Date: 10/31/13
 * Time: 3:01 PM
 */
public interface PolicyHandlerIface {

    /**
     * @param policy
     * @param user
     * @throws KustvaktException
     */
    int createPolicy(SecurityPolicy policy, User user) throws KustvaktException;

    /**
     * @param target
     * @param user
     * @param perm
     * @return
     */
    List<SecurityPolicy>[] getPolicies(Integer target, User user, Byte perm);

    List<SecurityPolicy> getPolicies(PolicyCondition condition,Class<? extends KustvaktResource> clazz, Byte perm);

    /**
     * @param policy
     * @return
     * @throws KustvaktException
     */
    void mapConstraints(SecurityPolicy policy) throws KustvaktException;

    /**
     * @param target
     * @param user
     * @param perm
     * @return
     */
    List<SecurityPolicy>[] getPolicies(String target, User user, Byte perm);

    /**
     * @param path
     * @param user
     * @param perm
     * @return
     */
    // todo: refactor
    List<SecurityPolicy>[] findPolicies(String path, User user, Byte perm);

    /**
     * @param path
     * @param user
     * @param b
     * @param clazz
     * @return
     * @throws KustvaktException
     */
    List<KustvaktResource.Container> getDescending(String path, User user,
            Byte b, Class<? extends KustvaktResource> clazz)
            throws KustvaktException;

    /**
     * @param path
     * @param user
     * @param b
     * @param clazz
     * @return
     * @throws KustvaktException
     */
    List<KustvaktResource.Container> getAscending(String path, User user,
            Byte b, Class<? extends KustvaktResource> clazz)
            throws KustvaktException;

    /**
     * @param id
     * @param user
     */
    int deleteResourcePolicies(String id, User user) throws KustvaktException;

    /**
     * @param policy
     * @param user
     * @return
     * @throws KustvaktException
     */
    int deletePolicy(SecurityPolicy policy, User user) throws KustvaktException;

    /**
     * @param policy
     * @param user
     * @return
     * @throws KustvaktException
     */
    int updatePolicy(SecurityPolicy policy, User user) throws KustvaktException;

    /**
     * checks if a similar policy already exists
     *
     * @param policy
     * @return
     * @throws KustvaktException
     */
    int checkPolicy(SecurityPolicy policy, User user) throws KustvaktException;

    /**
     * @param user
     * @param name
     * @param owner
     * @return
     * @throws KustvaktException
     */
    int matchCondition(User user, String name, boolean owner)
            throws KustvaktException;

    /**
     * @param username
     * @param condition
     * @param admin
     * @return
     * @throws KustvaktException
     */
    int addToCondition(String username, PolicyCondition condition,
            boolean admin) throws KustvaktException;

    /**
     * @param usernames
     * @param condition
     * @param status
     * @throws KustvaktException
     */

    //todo: add a handler user id, to skip the matching step in the corpusmanagement segment!
    int[] addToCondition(List<String> usernames, PolicyCondition condition,
            boolean status) throws KustvaktException;

    /**
     * @param usernames
     * @param condition
     * @throws KustvaktException
     */
    void removeFromCondition(List<String> usernames, PolicyCondition condition)
            throws KustvaktException;

    /**
     * @param param
     * @throws KustvaktException
     */
    int createParamBinding(Parameter param) throws KustvaktException;

    /**
     * @param condition
     * @return
     * @throws KustvaktException
     */
    List<String> getUsersFromCondition(PolicyCondition condition)
            throws KustvaktException;

    /**
     * @param policy
     * @throws KustvaktException
     */
    int removeParamBinding(SecurityPolicy policy) throws KustvaktException;

    int size();

}
