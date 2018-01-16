package de.ids_mannheim.korap.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.exceptions.KustvaktException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupMemberDaoTest {

    @Autowired
    private UserGroupMemberDao dao;

    @Test
    public void testRetrieveMemberByRole () throws KustvaktException {
        // dory group
        List<UserGroupMember> vcaAdmins = dao.retrieveMemberByRole(2,
                PredefinedRole.VC_ACCESS_ADMIN.getId());
//        System.out.println(vcaAdmins);
        assertEquals(1, vcaAdmins.size());
        assertEquals("dory", vcaAdmins.get(0).getUserId());
    }
}
