package com.yeahmobi.yscheduler.model.service.impl;

import com.yeahmobi.yscheduler.common.Constants;
import com.yeahmobi.yscheduler.common.Paginator;
import com.yeahmobi.yscheduler.model.Agent;
import com.yeahmobi.yscheduler.model.Task;
import com.yeahmobi.yscheduler.model.common.NameValuePair;
import com.yeahmobi.yscheduler.model.common.Query;
import com.yeahmobi.yscheduler.model.service.TaskService;
import com.yeahmobi.yscheduler.model.type.DependingStatus;
import com.yeahmobi.yscheduler.model.type.TaskStatus;
import com.yeahmobi.yscheduler.model.type.TaskType;
import com.yeahmobi.yunit.DbUnitTestExecutionListener;
import com.yeahmobi.yunit.annotation.DatabaseSetup;
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
 * Leo Liang
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-test.xml"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class})
public class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Date DEFAULT_TIME;

    static {
        try {
            DEFAULT_TIME = sdf.parse("2009-09-09 00:00:00");
        } catch (ParseException e) {
            // ignore it
        }
    }

    @Test
    @DatabaseSetup
    public void testGetById() throws Exception {
        Task task = this.taskService.get(1L);
        assertTask(task, 1L, 1L);

    }

    @Test
    @DatabaseSetup
    public void testGetByName() throws Exception {
        Task task = this.taskService.get("test1");
        assertTask(task, 1L, 1L);

        Assert.assertNull(this.taskService.get("fwoejiowjfowej"));
    }

    @Test
    @DatabaseSetup
    public void testListWithPagination() throws Exception {
        Paginator paginator = new Paginator();
        List<Task> tasks = this.taskService.list(new Query(), 1, paginator, 1);
        Assert.assertEquals(10, tasks.size());
        for (int i = 1; i <= tasks.size(); i++) {
            assertTask(tasks.get(i - 1), i, 1);
        }
    }

    @Test
    @DatabaseSetup
    public void testListWithPaginationAndWithAuthCheck() throws Exception {
        Paginator paginator = new Paginator();
        List<Task> tasks = this.taskService.list(new Query(), 2, paginator, 1);
        Assert.assertEquals(2, tasks.size());
        assertTask(tasks.get(0), 11, 1);
        assertTask(tasks.get(1), 13, 2);
    }

    @Test
    @DatabaseSetup
    public void testListWithPaginationAndWithQuery() throws Exception {
        Paginator paginator = new Paginator();
        Query query = new Query();
        query.setName("test");
        query.setOwner(1L);
        query.setTaskStatus(TaskStatus.OPEN);
        query.setTaskType(TaskType.SHELL);

        // 11条
        {
            List<Task> tasks = this.taskService.list(query, 1, paginator, 1);
            Assert.assertEquals(10, tasks.size());
        }
        {
            List<Task> tasks = this.taskService.list(query, 2, paginator, 1);
            Assert.assertEquals(1, tasks.size());
        }

        // 0条,被权限过滤
        {
            List<Task> tasks = this.taskService.list(query, 1, paginator, 3);
            Assert.assertEquals(0, tasks.size());
        }
    }

    @Test
    @DatabaseSetup
    public void testAdd() throws Exception {
        Task task = new Task();
        task.setAgentId(1L);
        task.setCommand("lsls");
        task.setCrontab("0/1 * * * * ?");
        task.setDescription("test");
        task.setName("test1");
        task.setOwner(1L);
        task.setRetryTimes(2);
        task.setStatus(TaskStatus.OPEN);
        task.setTimeout(100);
        task.setType(TaskType.HADOOP);
        task.setCanSkip(false);

        this.taskService.add(task);
        Date nowTime = new Date();
        Long id = task.getId();

        Assert.assertNotNull(id);
        Task actual = this.taskService.get(id);
        assertTask(actual, id, task.getAgentId(), task.getName(), task.getCommand(), nowTime, nowTime, task.getCrontab(), task.getDescription(), nowTime,
                task.getOwner(), task.getRetryTimes(), task.getStatus(), task.getTimeout(), task.getType(), false, DependingStatus.NONE);

    }

    @Test
    @DatabaseSetup
    public void testUpdate() throws Exception {
        Task task = new Task();
        task.setId(15L);
        task.setAgentId(2L);
        task.setCommand("lsls");
        task.setCrontab("0/1 * * * * ?");
        task.setDescription("test");
        task.setName("test1");
        task.setOwner(2L);
        task.setRetryTimes(2);
        task.setStatus(TaskStatus.OPEN);
        task.setTimeout(100);
        task.setType(TaskType.HADOOP);
        this.taskService.update(task);

        Date nowTime = new Date();

        Task actual = this.taskService.get(15L);
        assertTask(actual, 15L, task.getAgentId(), task.getName(), task.getCommand(), DEFAULT_TIME, nowTime, task.getCrontab(), task.getDescription(),
                nowTime, task.getOwner(), task.getRetryTimes(), task.getStatus(), task.getTimeout(), task.getType(), true, DependingStatus.NONE);
        task.setAgentId(1l);
        this.taskService.updateAgentId(15l, 1l);
        actual = this.taskService.get(15L);
        assertTask(actual, 15L, task.getAgentId(), task.getName(), task.getCommand(), DEFAULT_TIME, nowTime, task.getCrontab(), task.getDescription(),
                nowTime, task.getOwner(), task.getRetryTimes(), task.getStatus(), task.getTimeout(), task.getType(), true, DependingStatus.NONE);
        task.setAttachment("attachment");
        task.setAttachmentVersion(1l);
        this.taskService.updateAttachment(15l, "attachment", 1l);
        actual = this.taskService.get(15L);
        assertTask(actual, 15L, task.getAgentId(), task.getName(), task.getCommand(), DEFAULT_TIME, nowTime, task.getCrontab(), task.getDescription(),
                nowTime, task.getOwner(), task.getRetryTimes(), task.getStatus(), task.getTimeout(), task.getType(), true, DependingStatus.NONE);

    }

    @Test
    @DatabaseSetup
    public void testUpdateAgentAndAttachment() throws Exception {
        Task task = new Task();
        task.setId(15L);
        task.setAgentId(2L);
        task.setCommand("lsls");
        task.setCrontab("0/1 * * * * ?");
        task.setDescription("test");
        task.setName("test1");
        task.setOwner(2L);
        task.setRetryTimes(2);
        task.setStatus(TaskStatus.OPEN);
        task.setTimeout(100);
        task.setType(TaskType.HADOOP);
        this.taskService.update(task);

        Date nowTime = new Date();

        Task actual = this.taskService.get(15L);
        assertTask(actual, 15L, task.getAgentId(), task.getName(), task.getCommand(), DEFAULT_TIME, nowTime, task.getCrontab(), task.getDescription(),
                nowTime, task.getOwner(), task.getRetryTimes(), task.getStatus(), task.getTimeout(), task.getType(), true, DependingStatus.NONE);
    }

    @Test
    @DatabaseSetup
    public void testListByUser() throws Exception {
        List<NameValuePair> list = this.taskService.list(1L);
        Assert.assertEquals(12, list.size());
    }

    @Test
    @DatabaseSetup
    public void testCanModify() throws Exception {
        Assert.assertTrue(this.taskService.canModify(1, 1));
        Assert.assertTrue(this.taskService.canModify(1, 2));
        Assert.assertTrue(this.taskService.canModify(13, 1));
        Assert.assertFalse(this.taskService.canModify(14, 2));
        Assert.assertFalse(this.taskService.canModify(15, 1));
        Assert.assertFalse(this.taskService.canModify(1000, 1));
    }

    @Test
    @DatabaseSetup
    public void testNameExists() throws Exception {
        Assert.assertTrue(this.taskService.nameExist("test1"));
        Assert.assertFalse(this.taskService.nameExist("11111"));
    }

    @Test
    @DatabaseSetup
    public void testListAll() throws Exception {
        Assert.assertEquals(13, this.taskService.list(TaskStatus.OPEN).size());
        Assert.assertEquals(1, this.taskService.list(TaskStatus.PAUSED).size());
        Assert.assertEquals(1, this.taskService.list(TaskStatus.REMOVED).size());
    }

    @Test
    @DatabaseSetup
    public void testUpdateLastScheduleTime() throws Exception {
        Task oldTask = this.taskService.get(1L);
        Date now = new Date();
        this.taskService.updateLastScheduleTime(1L, now);

        Task newTask = this.taskService.get(1L);
        assertTask(newTask, 1L, oldTask.getAgentId(), oldTask.getName(), oldTask.getCommand(), oldTask.getCreateTime(), oldTask.getUpdateTime(), oldTask
                .getCrontab(), oldTask.getDescription(), now, oldTask.getOwner(), oldTask.getRetryTimes(), oldTask.getStatus(), oldTask.getTimeout(), oldTask
                .getType(), true, DependingStatus.NONE);
    }

    @Test
    @DatabaseSetup
    public void testHasTaskAttachedToAgent() throws Exception {
        Assert.assertTrue(this.taskService.hasTaskAttachedToAgent(1L));
        Assert.assertFalse(this.taskService.hasTaskAttachedToAgent(100L));
    }

    @Test
    @DatabaseSetup
    public void testAddHeartbeatTaskAndListHeartbeatTasks() throws Exception {
        Agent agent = new Agent();
        agent.setId(100L);
        agent.setName("test100");
        this.taskService.addHeartbeatTask(agent);

        List<Task> heartbeatTasks = this.taskService.listHeartbeatTask();
        Date nowTime = new Date();
        Assert.assertEquals(1, heartbeatTasks.size());
        Task heartbeatTask = heartbeatTasks.get(0);
        assertTask(heartbeatTask, heartbeatTask.getId(), 100L, Constants.HEARTBEAT_TASK_NAME_PREFIX + agent.getName(), "curl http://127.0.0" +
                ".1:8080/heartbeat/agent/active?agentId=" + agent.getId(), nowTime, nowTime, "0 * * * * *", "Hearbeat task for agent test100", nowTime, 2L,
                5, TaskStatus.OPEN, 1, TaskType.SHELL, true, DependingStatus.NONE);
    }

    @Test
    @DatabaseSetup
    public void testRemoveHeartbeatTask() throws Exception {
        Agent agent = new Agent();
        agent.setId(100L);
        agent.setName("test100");
        this.taskService.addHeartbeatTask(agent);
        this.taskService.removeHeartbeatTask(100L, "test100");
        Assert.assertEquals(0, this.taskService.listHeartbeatTask().size());
    }

    @Test
    @DatabaseSetup
    public void testAddUpgradeTaskAndListUpgradeTasks() throws Exception {
        Agent agent = new Agent();
        agent.setId(100L);
        agent.setName("test100");
        this.taskService.addUpgradeTask(agent);

        Task upgradeTask = this.taskService.get(agent.getUpgradeTaskId());

        Assert.assertNotNull(upgradeTask);
        Assert.assertEquals(agent.getId(), upgradeTask.getAgentId());
    }

    @Test
    @DatabaseSetup
    public void testRemoveTask() throws Exception {
        long id = 1L;
        this.taskService.removeTask(id);
        Assert.assertNull(this.taskService.get(id));
    }

    @Test
    @DatabaseSetup
    public void testTeamRootTask() {
        // 测试add和get
        this.taskService.addRootTaskIfAbsent("test");
        Task task = this.taskService.getRootTask("test");
        Assert.assertEquals("test_root_node", task.getName());
        this.taskService.addRootTaskIfAbsent("");
        task = this.taskService.getRootTask("");
        Assert.assertEquals("root_node", task.getName());

        // 测试update
        this.taskService.updateTeamRootTaskName("test", "team");
        task = this.taskService.getRootTask("test");
        Assert.assertNull(task);
        task = this.taskService.getRootTask("team");
        Assert.assertNotNull(task);

        // update一个不存在的team
        this.taskService.updateTeamRootTaskName("test1", "team1");
        task = this.taskService.getRootTask("test");
        Assert.assertNull(task);
        task = this.taskService.getRootTask("team1");
        Assert.assertNotNull(task);
    }

    private void assertTask(Task task, long id, long owner) {
        assertTask(task, id, 1, "test" + id, "echo test" + id, DEFAULT_TIME, DEFAULT_TIME, "0/5 * * * * ?", "desc..." + id, DEFAULT_TIME, owner, 1,
                TaskStatus.OPEN, 1, TaskType.SHELL, true, DependingStatus.NONE);
    }

    private void assertTask(Task task, long id, long agentId, String name, String command, Date createTime, Date updateTime, String crontab, String desc,
                            Date lastScheduleTime, long owner, Integer retryTimes, TaskStatus status, Integer timeout, TaskType type, Boolean canSkip,
                            DependingStatus dependingStatus) {
        Assert.assertEquals(Long.valueOf(id), task.getId());
        Assert.assertEquals(Long.valueOf(agentId), task.getAgentId());
        Assert.assertEquals(command, task.getCommand());
        TestUtils.generallyEquals(createTime, task.getCreateTime());
        TestUtils.generallyEquals(updateTime, task.getUpdateTime());
        Assert.assertEquals(crontab, task.getCrontab());
        Assert.assertEquals(desc, task.getDescription());
        if (canSkip == null) {
            Assert.assertNull(task.getCanSkip());
        } else {
            Assert.assertEquals(canSkip, task.getCanSkip());
        }
        if (lastScheduleTime == null) {
            Assert.assertNull(task.getLastScheduleTime());
        } else {
            TestUtils.generallyEquals(lastScheduleTime, task.getLastScheduleTime());
        }
        Assert.assertEquals(dependingStatus, task.getLastStatusDependency());
        Assert.assertEquals(name, task.getName());
        Assert.assertEquals(Long.valueOf(owner), task.getOwner());
        Assert.assertEquals(retryTimes, task.getRetryTimes());
        Assert.assertEquals(status, task.getStatus());
        Assert.assertEquals(timeout, task.getTimeout());
        Assert.assertEquals(type, task.getType());
    }

}
