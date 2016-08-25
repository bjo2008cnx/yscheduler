package com.yeahmobi.yscheduler.model.service.impl;

import com.yeahmobi.yscheduler.common.Paginator;
import com.yeahmobi.yscheduler.common.PasswordEncoder;
import com.yeahmobi.yscheduler.model.User;
import com.yeahmobi.yscheduler.model.common.NameValuePair;
import com.yeahmobi.yscheduler.model.service.UserService;
import com.yeahmobi.yunit.DbUnitTestExecutionListener;
import com.yeahmobi.yunit.annotation.DatabaseSetup;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author atell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
public class UserServiceImplTest {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserService                   userService;

    @Test
    @DatabaseSetup
    public void testGet() throws Exception {
        long id = 1;
        User user = this.userService.get(id);

        assertUser(user, 1);
    }

    @Test
    @DatabaseSetup
    public void testList() throws Exception {
        // page 1
        int pageNum = 1;
        Paginator paginator = new Paginator();
        List<User> list = this.userService.list(pageNum, paginator);

        Assert.assertTrue(list.size() == 10);
        for (int i = 0; i < list.size(); i++) {
            User user = list.get(i);
            assertUser(user, i + 1);
        }
        Assert.assertEquals(15, paginator.getItems());
        Assert.assertEquals(2, paginator.getPages());
        Assert.assertEquals(1, paginator.getPage());
        // page 2
        pageNum = 2;
        paginator = new Paginator();
        list = this.userService.list(pageNum, paginator);

        Assert.assertTrue(list.size() == 5);
        for (int i = 0; i < list.size(); i++) {
            User user = list.get(i);
            assertUser(user, i + 11);
        }
        Assert.assertEquals(15, paginator.getItems());
        Assert.assertEquals(2, paginator.getPages());
        Assert.assertEquals(2, paginator.getPage());

    }

    @Test
    @DatabaseSetup
    public void testListByTeamPagination() throws Exception {
        // page 1
        int pageNum = 1;
        Paginator paginator = new Paginator();
        List<User> list = this.userService.listByTeam(1L, pageNum, paginator);

        Assert.assertTrue(list.size() == 1);
        assertUser(list.get(0), 1);
        Assert.assertEquals(1, paginator.getItems());
        Assert.assertEquals(1, paginator.getPages());
        Assert.assertEquals(1, paginator.getPage());

    }

    @Test
    @DatabaseSetup
    public void testListByTeamAll() throws Exception {
        for (long i = 1; i <= 15; i++) {
            User user = new User();
            user.setId(i);
            user.setTeamId(100L);
            this.userService.update(user);
        }

        List<User> users = this.userService.listByTeam(100L);
        Assert.assertTrue(users.size() == 15);
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            assertUser(user, i + 1, 100L);
        }
    }

    @Test
    @DatabaseSetup
    public void testListPairs() throws Exception {
        List<NameValuePair> list = this.userService.list();

        Assert.assertTrue(list.size() == 15);
        for (int i = 0; i < list.size(); i++) {
            NameValuePair pair = list.get(i);
            Assert.assertEquals("admin" + (i + 1), pair.getName());
            Assert.assertEquals((i + 1), pair.getValue());
        }
    }

    @Test
    @DatabaseSetup
    public void testGetByName() throws Exception {
        String username = "admin1";
        User user = this.userService.get(username);

        assertUser(user, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    @DatabaseSetup
    public void testGetByNameNotExists() throws Exception {
        String username = "admin0";
        this.userService.get(username);
    }

    @Test
    @DatabaseSetup
    public void testAdd() throws Exception {
        User user = new User();
        user.setName("admin16");
        user.setPassword(PasswordEncoder.encode("admin"));
        user.setTelephone("15921096896");
        user.setEmail("platform@ndpmedia.com");
        user.setUpdateTime(new Date());

        this.userService.add(user);
        Long id = user.getId();

        Assert.assertNotNull(id);
        User actual = this.userService.get(id);
        assertUser(actual, id, user.getName(), user.getPassword(), user.getEmail(), user.getTelephone(),
                   user.getToken(), user.getTeamId(), new Date(), new Date());

    }

    @Test(expected = IllegalArgumentException.class)
    @DatabaseSetup
    public void testAddDuplicate() throws Exception {
        User user = new User();
        user.setName("admin1");
        user.setPassword(PasswordEncoder.encode("admin"));
        user.setTelephone("15921096896");
        user.setEmail("platform@ndpmedia.com");
        user.setUpdateTime(new Date());

        this.userService.add(user);
    }

    @Test
    @DatabaseSetup
    public void testUpdate() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("admin0");
        user.setPassword(PasswordEncoder.encode("admin0"));
        user.setTelephone("15921096897");
        user.setEmail("platform2@ndpmedia.com");
        user.setTeamId(100L);
        user.setCreateTime(new Date());
        Date updateTime = new Date();
        user.setUpdateTime(updateTime);

        this.userService.update(user);

        Long id = user.getId();

        User actual = this.userService.get(id);
        assertUser(actual, id, user.getName(), user.getPassword(), user.getEmail(), user.getTelephone(), "token1",
                   user.getTeamId(), user.getCreateTime(), user.getUpdateTime());

    }

    @Test
    @DatabaseSetup
    public void testRemove() throws Exception {
        long userId = 1;
        this.userService.remove(userId);

        User actual = this.userService.get(userId);

        Assert.assertNull(actual);

    }

    @Test
    @DatabaseSetup
    public void testResetPassword() throws Exception {
        long userId = 1;
        this.userService.resetPassword(userId);

        User actual = this.userService.get(userId);

        Assert.assertTrue(TestUtils.generallyEquals(new Date(), actual.getUpdateTime()));
        Assert.assertEquals(PasswordEncoder.encode("admin1"), actual.getPassword());

    }

    @Test(expected = IllegalArgumentException.class)
    @DatabaseSetup
    public void testRegenTokenWithUnExistsUserId() throws Exception {
        this.userService.regenToken(100L);

    }

    @Test
    @DatabaseSetup
    public void testRegenToken() throws Exception {
        long userId = 1;
        this.userService.regenToken(userId);

        User actual = this.userService.get(userId);

        Assert.assertTrue(TestUtils.generallyEquals(new Date(), actual.getUpdateTime()));
        Assert.assertTrue(StringUtils.isNotBlank(actual.getToken()));
        Assert.assertNotSame("token1", actual.getToken());

    }

    @Test
    @DatabaseSetup
    public void testHasTeamUser() throws Exception {
        Assert.assertTrue(this.userService.hasTeamUser(1L));
        Assert.assertFalse(this.userService.hasTeamUser(100L));
    }

    private void assertUser(User user, long id, long teamId) throws ParseException {
        assertUser(user, id, "admin" + id, "21232f297a57a5a743894a0e4a801fc3", "platform@ndpmedia.com", "15921096896",
                   "token" + id, teamId, "2014-11-26 17:00:00", "2014-11-26 17:38:00");
    }

    private void assertUser(User user, long id) throws ParseException {
        assertUser(user, id, "admin" + id, "21232f297a57a5a743894a0e4a801fc3", "platform@ndpmedia.com", "15921096896",
                   "token" + id, id, "2014-11-26 17:00:00", "2014-11-26 17:38:00");
    }

    private void assertUser(User user, long id, String name, String password, String email, String telephone,
                            String token, Long teamId, String createTime, String updateTime) throws ParseException {
        assertUser(user, id, name, password, email, telephone, token, teamId, sdf.parse(createTime),
                   sdf.parse(updateTime));
    }

    private void assertUser(User user, long id, String name, String password, String email, String telephone,
                            String token, Long teamId, Date createTime, Date updateTime) throws ParseException {
        Assert.assertEquals(Long.valueOf(id), user.getId());
        Assert.assertEquals(name, user.getName());
        Assert.assertEquals(password, user.getPassword());
        Assert.assertEquals(email, user.getEmail());
        Assert.assertEquals(telephone, user.getTelephone());
        Assert.assertEquals(token, user.getToken());
        if (teamId != null) {
            Assert.assertEquals(Long.valueOf(teamId), user.getTeamId());
        } else {
            Assert.assertNull(user.getTeamId());
        }
        Assert.assertTrue(TestUtils.generallyEquals(createTime, user.getCreateTime()));
        Assert.assertTrue(TestUtils.generallyEquals(updateTime, user.getUpdateTime()));
    }

}
