package de.ids_mannheim.korap.web.controller;

import java.util.List;

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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Controller
@Path("{version}/admin/group")
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
            return service.retrieveUserGroupByStatus(username, status);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Retrieves a specific user-group. Only system admins are
     * allowed.
     * 
     * @param securityContext
     * @param groupName
     *            group name
     * @return a user-group
     */
    @POST
    @Path("@{groupName}")
    public UserGroupDto retrieveUserGroup (
            @PathParam("groupName") String groupName) {
        try {
            return service.searchByName(groupName);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

    }

}
