package com.yeahmobi.yscheduler.model.service.impl;

import com.yeahmobi.yscheduler.common.Constants;
import com.yeahmobi.yscheduler.common.Paginator;
import com.yeahmobi.yscheduler.model.Agent;
import com.yeahmobi.yscheduler.model.Task;
import com.yeahmobi.yscheduler.model.service.AgentService;
import com.yeahmobi.yscheduler.model.service.TaskService;
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
public class AgentServiceImplTest {

    @Autowired
    private AgentService agentService;

    @Autowired
    private TaskService taskService;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Date DEFAULT_TIME;

    static {
        try {
            DEFAULT_TIME = sdf.parse("2014-11-26 17:38:00");
        } catch (ParseException e) {
            // ignore it
        }
    }

    @Test
    @DatabaseSetup
    public void testGetById() throws Exception {
        Agent agent = this.agentService.get(1);

        assertAgent(agent, 1L);
    }

    @Test
    @DatabaseSetup
    public void testGetByName() throws Exception {
        Agent agent = this.agentService.get("agent1");

        assertAgent(agent, 1L);

        Assert.assertNull(this.agentService.get("feiwjfoewo"));
    }

    private void assertAgent(Agent agent, Long id) {
        assertAgent(agent, id, "agent" + id, true, true, "10.0.0." + id, DEFAULT_TIME, DEFAULT_TIME);
    }

    private void assertAgent(Agent agent, Long id, String name, boolean alive, boolean enable, String ip, Date createTime, Date updateTime) {
        Assert.assertEquals(id, agent.getId());
        Assert.assertEquals(name, agent.getName());
        Assert.assertEquals(alive, agent.getAlive());
        Assert.assertEquals(enable, agent.getEnable());
        Assert.assertEquals(ip, agent.getIp());
        TestUtils.generallyEquals(createTime, agent.getCreateTime());
        TestUtils.generallyEquals(updateTime, agent.getUpdateTime());
    }

    @Test
    @DatabaseSetup
    public void testList() throws Exception {
        List<Agent> agents = this.agentService.list();
        Assert.assertEquals(11, agents.size());
        for (int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            assertAgent(agent, (long) (i + 1));
        }
    }

    @Test
    @DatabaseSetup
    public void testListWithPagination() throws Exception {
        Paginator paginator = new Paginator();
        List<Agent> agents = this.agentService.list(2, paginator);
        Assert.assertEquals(1, agents.size());
        assertAgent(agents.get(0), 11L);
    }

    @Test
    @DatabaseSetup
    public void testListByTeam() throws Exception {
        {
            long teamId = 1;
            boolean enable = true;
            List<Agent> agents = this.agentService.list(teamId, enable);
            Assert.assertEquals(11, agents.size());
            for (int i = 0; i < agents.size(); i++) {
                Agent agent = agents.get(i);
                assertAgent(agent, (long) (i + 1));
            }
        }
        {
            long teamId = 2;
            boolean enable = true;
            List<Agent> agents = this.agentService.list(teamId, enable);
            Assert.assertEquals(0, agents.size());
        }
        {
            long teamId = 1;
            boolean enable = false;
            List<Agent> agents = this.agentService.list(teamId, enable);
            Assert.assertEquals(0, agents.size());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @DatabaseSetup
    public void testAddWithNameExists() throws Exception {
        Agent agent = new Agent();
        agent.setName("agent1");
        agent.setIp("1.1.1.1");
        this.agentService.add(agent);
    }

    @Test()
    @DatabaseSetup
    public void testAdd() throws Exception {
        Agent agent = new Agent();
        agent.setName("agent100");
        agent.setIp("1.1.1.1");
        this.agentService.add(agent);

        Date now = new Date();

        Long id = agent.getId();
        Assert.assertNotNull(id);

        Agent actual = this.agentService.get(id);

        Assert.assertEquals(agent.getName(), actual.getName());
        Assert.assertEquals(agent.getIp(), actual.getIp());
        Assert.assertEquals(true, actual.getAlive());
        Assert.assertEquals(true, actual.getEnable());
        Assert.assertEquals(sdf.format(now), sdf.format(actual.getCreateTime()));
        Assert.assertEquals(sdf.format(now), sdf.format(actual.getUpdateTime()));

        List<Task> tasks = this.taskService.list(TaskStatus.OPEN);
        Task heartbeatTask = null;
        for (Task task : tasks) {
            if ((Constants.HEARTBEAT_TASK_NAME_PREFIX + agent.getName()).equals(task.getName())) {
                heartbeatTask = task;
                break;
            }
        }
        Assert.assertNotNull(heartbeatTask);
        Assert.assertEquals(id, heartbeatTask.getAgentId());
        Assert.assertEquals(Constants.HEARTBEAT_TASK_NAME_PREFIX + agent.getName(), heartbeatTask.getName());
        Assert.assertEquals("0 * * * * *", heartbeatTask.getCrontab());
        Assert.assertEquals("curl http://127.0.0.1:8080/heartbeat/agent/active?agentId=" + agent.getId(), heartbeatTask.getCommand());
        Assert.assertEquals(sdf.format(now), sdf.format(heartbeatTask.getCreateTime()));
        Assert.assertEquals(sdf.format(now), sdf.format(heartbeatTask.getUpdateTime()));
        Assert.assertEquals(sdf.format(now), sdf.format(heartbeatTask.getLastScheduleTime()));
        Assert.assertEquals(Long.valueOf(2), heartbeatTask.getOwner());
        Assert.assertEquals(Integer.valueOf(5), heartbeatTask.getRetryTimes());
        Assert.assertEquals(TaskStatus.OPEN, heartbeatTask.getStatus());
        Assert.assertEquals(TaskType.SHELL, heartbeatTask.getType());

        // 验证upgradeTask存在
        Task upgradeTask = this.taskService.get(actual.getUpgradeTaskId());
        Assert.assertNotNull(upgradeTask);
    }

    @Test
    @DatabaseSetup
    public void testUpdateIp() throws Exception {
        this.agentService.update(1, "1.1.1.1", 1);

        Agent agent = this.agentService.get(1);
        assertAgent(agent, 1L, "agent1", true, true, "1.1.1.1", DEFAULT_TIME, new Date());
    }

    @Test
    @DatabaseSetup
    public void testUpdateVersion() throws Exception {
        this.agentService.updateVersion(1, "0.5.0");
        Agent agent = this.agentService.get(1);
        Assert.assertEquals("0.5.0", agent.getVersion());
        assertAgent(agent, 1L, "agent1", true, true, "10.0.0.1", DEFAULT_TIME, new Date());
    }

    @Test
    @DatabaseSetup
    public void testListInPlatform() throws Exception {
        List<Agent> platformAgents = this.agentService.listInPlatform();

        Assert.assertEquals(0, platformAgents.size());

        Agent platformAgent = new Agent();
        platformAgent.setIp("1.1.1.1");
        platformAgent.setName("platform11");

        this.agentService.add(platformAgent);

        platformAgents = this.agentService.listInPlatform();
        Assert.assertEquals(1, platformAgents.size());
        Date now = new Date();
        assertAgent(platformAgents.get(0), 12L, "platform11", true, true, "1.1.1.1", now, now);
    }

    @Test(expected = IllegalArgumentException.class)
    @DatabaseSetup
    public void testRemoveAgentWithOpenTask() throws Exception {
        Task task = new Task();
        task.setAgentId(1L);
        task.setCommand("ls");
        task.setName("testTask");
        task.setOwner(1L);
        task.setStatus(TaskStatus.OPEN);
        task.setType(TaskType.SHELL);
        task.setTimeout(0);
        task.setRetryTimes(0);
        task.setCanSkip(false);
        this.taskService.add(task);

        this.agentService.remove(1L, "agent1");
    }

    @Test(expected = IllegalArgumentException.class)
    @DatabaseSetup
    public void testRemoveAgentWithPausedTask() throws Exception {
        Task task = new Task();
        task.setAgentId(1L);
        task.setCommand("ls");
        task.setName("testTask");
        task.setOwner(1L);
        task.setStatus(TaskStatus.PAUSED);
        task.setType(TaskType.SHELL);
        task.setTimeout(0);
        task.setRetryTimes(0);
        task.setCanSkip(false);
        this.taskService.add(task);

        this.agentService.remove(1L, "agent1");
    }

    @Test
    @DatabaseSetup
    public void testRemoveAgentWithRemovedTask() throws Exception {
        Task task = new Task();
        task.setAgentId(2L);
        task.setCommand("ls");
        task.setName("testTask");
        task.setOwner(1L);
        task.setStatus(TaskStatus.REMOVED);
        task.setType(TaskType.SHELL);
        task.setTimeout(0);
        task.setRetryTimes(0);
        task.setCanSkip(false);
        this.taskService.add(task);

        this.agentService.remove(2L, "agent2");

        Assert.assertNull(this.agentService.get(2L));
    }

}
