package de.ids_mannheim.korap.web.input;

import de.ids_mannheim.korap.web.controller.UserGroupController;
import lombok.Getter;
import lombok.Setter;

/** Java POJO of JSON input used in the user group controller for 
 * creating user group and managing group members.
 * 
 * @author margaretha
 * @see UserGroupController
 */
@Deprecated
@Getter
@Setter
public class UserGroupJson {

    private int id;
    private String name;
    private String[] members;
}
