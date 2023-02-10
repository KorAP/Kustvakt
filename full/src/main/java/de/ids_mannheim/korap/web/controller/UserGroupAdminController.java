package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AdminFilter;
import de.ids_mannheim.korap.web.utils.ResourceFilters;

@Controller
@Path("{version}/group/admin")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ APIVersionFilter.class, AdminFilter.class })
public class UserGroupAdminController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private UserGroupService service;
    
    /**
     * Lists user-groups for system-admin purposes. If username is
     * specified, lists user-groups of the given user, otherwise list
     * user-groups of all users. If status specified, list only
     * user-groups with the given status, otherwise list user-groups
     * regardless of their status.
     * 
     * @param securityContext
     * @param username
     *            a username
     * @param status
     *            {@link UserGroupStatus}
     * @return a list of user-groups
     */
    @POST
    @Path("list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public List<UserGroupDto> listUserGroupBySystemAdmin (
            @FormParam("username") String username,
            @FormParam("status") UserGroupStatus status) {
        try {
            return service.retrieveUserGroupByStatus(username,
                    status);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

}
