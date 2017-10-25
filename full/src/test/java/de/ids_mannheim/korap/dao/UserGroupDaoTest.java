package de.ids_mannheim.korap.dao;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.entity.UserGroupMember;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
public class UserGroupDaoTest {

    @Autowired
    private UserGroupDao dao; 
    
    @Autowired
    protected ApplicationContext context;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    public void testNewGroup () {

        

    }
}
