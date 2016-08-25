package com.yeahmobi.yscheduler.model.service.impl;

import com.yeahmobi.yscheduler.common.Paginator;
import com.yeahmobi.yscheduler.model.Task;
import com.yeahmobi.yscheduler.model.TaskInstance;
import com.yeahmobi.yscheduler.model.common.Query;
import com.yeahmobi.yscheduler.model.common.Query.TaskScheduleType;
import com.yeahmobi.yscheduler.model.service.TaskInstanceService;
import com.yeahmobi.yscheduler.model.type.TaskInstanceStatus;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Leo.Liang
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-test.xml"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class})
public class TaskInstanceServiceImplTest {

    @Autowired
    private TaskInstanceService instanceService;

    private static final long TIME_ACCEPTABLE_DIFF = 1000L;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    @DatabaseSetup
    public void testGet() throws Exception {
        TaskInstance instance = this.instanceService.get(1);
        assertTaskInstance(instance, 1L, sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00", "2014-11-26 17:38:10",
                null, TaskInstanceStatus.RUNNING, 1L, 1L);
    }

    @Test
    @DatabaseSetup
    public void testGetByTaskIdAndWorkflowInstanceId() throws Exception {
        TaskInstance instance = this.instanceService.get(1, 1);
        assertTaskInstance(instance, 1L, sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00", "2014-11-26 17:38:10",
                null, TaskInstanceStatus.RUNNING, 1L, 1L);
        Assert.assertNull(this.instanceService.get(100, 1000));
    }

    @Test
    @DatabaseSetup
    public void testGetLast() throws Exception {
        TaskInstance instance1 = this.instanceService.get(1);
        TaskInstance instance4 = this.instanceService.get(4);
        TaskInstance instance5 = this.instanceService.get(5);
        Task task = new Task();
        task.setId(1l);
        task.setCrontab("*/5 * * * *");
        assertTaskInstance(this.instanceService.getLast(task, instance4), instance1.getId(), instance1.getCreateTime(), instance1.getUpdateTime(), sdf.format
                (instance1.getScheduleTime()), sdf.format(instance1.getStartTime()), sdf.format(instance1.getEndTime()), instance1.getStatus(), instance1
                .getTaskId(), instance1.getWorkflowInstanceId());
        TaskInstance newTaskInstance = new TaskInstance();
        newTaskInstance.setScheduleTime(sdf.parse("2014-11-26 00:20:00"));

        assertTaskInstance(this.instanceService.getLast(task, newTaskInstance), instance5.getId(), instance5.getCreateTime(), instance5.getUpdateTime(), sdf
                .format(instance5.getScheduleTime()), sdf.format(instance5.getStartTime()), sdf.format(instance5.getEndTime()), instance5.getStatus(),
                instance5.getTaskId(), instance5.getWorkflowInstanceId());
        Assert.assertNull(this.instanceService.getLast(task, instance1));
        Assert.assertNull(this.instanceService.getLast(task, instance5));
        Assert.assertNull(this.instanceService.getLast(null, instance5));
    }

    @Test
    @DatabaseSetup
    public void testExistUncompletedScheduled() throws Exception {
        Assert.assertTrue(this.instanceService.existUncompletedScheduled(1L));
        Assert.assertTrue(this.instanceService.existUncompletedScheduled(2L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(3L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(4L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(5L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(6L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(7L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(8L));
        Assert.assertFalse(this.instanceService.existUncompletedScheduled(9L));
    }

    @Test
    @DatabaseSetup
    public void testListByWorkflowInstanceId() throws Exception {
        List<TaskInstance> taskInstances = this.instanceService.listByWorkflowInstanceId(21L);
        Assert.assertEquals(11, taskInstances.size());
        for (int i = 1; i <= taskInstances.size(); i++) {
            TaskInstance instance = taskInstances.get(i - 1);
            assertTaskInstance(instance, Long.valueOf(20 + i), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                    "2014-11-26 17:38:10", "2014-11-26 17:38:30", TaskInstanceStatus.SUCCESS, 21L, 21L);
        }
    }

    @Test
    @DatabaseSetup
    public void testListByWorkflowInstanceIdPagination() throws Exception {
        List<TaskInstance> taskInstances = this.instanceService.listByWorkflowInstanceId(21L, 1, new Paginator());
        Assert.assertEquals(10, taskInstances.size());
        for (int i = 1; i <= taskInstances.size(); i++) {
            TaskInstance instance = taskInstances.get(i - 1);
            assertTaskInstance(instance, Long.valueOf(20 + i), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                    "2014-11-26 17:38:10", "2014-11-26 17:38:30", TaskInstanceStatus.SUCCESS, 21L, 21L);
        }

        taskInstances = this.instanceService.listByWorkflowInstanceId(21L, 2, new Paginator());
        Assert.assertEquals(1, taskInstances.size());
        assertTaskInstance(taskInstances.get(0), Long.valueOf(31), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                "2014-11-26 17:38:10", "2014-11-26 17:38:30", TaskInstanceStatus.SUCCESS, 21L, 21L);
    }

    @Test
    @DatabaseSetup
    public void testListByTaskId() throws Exception {
        List<TaskInstance> taskInstances = this.instanceService.listAll(21L);
        Assert.assertEquals(11, taskInstances.size());
        for (int i = 1; i <= taskInstances.size(); i++) {
            TaskInstance instance = taskInstances.get(i - 1);
            assertTaskInstance(instance, Long.valueOf(32 - i), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                    "2014-11-26 17:38:10", "2014-11-26 17:38:30", TaskInstanceStatus.SUCCESS, 21L, 21L);
        }
    }

    @Test
    @DatabaseSetup
    public void testListByTaskIdPagination() throws Exception {
        List<TaskInstance> taskInstances = this.instanceService.list(new Query(), 21L, 1, new Paginator());
        Assert.assertEquals(10, taskInstances.size());
        for (int i = 1; i <= taskInstances.size(); i++) {
            TaskInstance instance = taskInstances.get(i - 1);
            assertTaskInstance(instance, Long.valueOf(32 - i), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                    "2014-11-26 17:38:10", "2014-11-26 17:38:30", TaskInstanceStatus.SUCCESS, 21L, 21L);
        }

        taskInstances = this.instanceService.list(new Query(), 21L, 2, new Paginator());
        Assert.assertEquals(1, taskInstances.size());
        assertTaskInstance(taskInstances.get(0), Long.valueOf(21), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                "2014-11-26 17:38:10", "2014-11-26 17:38:30", TaskInstanceStatus.SUCCESS, 21L, 21L);
    }

    @Test
    @DatabaseSetup
    public void testListByTaskIdPaginationWithQuery() throws Exception {
        // WORKFLOW_SCHEDULED
        {
            Query query = new Query();
            query.setTaskInstanceStatus(TaskInstanceStatus.SUCCESS);
            query.setTaskScheduleType(TaskScheduleType.WORKFLOW_SCHEDULED);
            List<TaskInstance> taskInstances = this.instanceService.list(query, 21L, 1, new Paginator());
            Assert.assertEquals(3, taskInstances.size());
        }
        // manual
        {
            Query query = new Query();
            query.setTaskInstanceStatus(TaskInstanceStatus.SUCCESS);
            query.setTaskScheduleType(TaskScheduleType.MANAUAL);
            List<TaskInstance> taskInstances = this.instanceService.list(query, 21L, 1, new Paginator());
            Assert.assertEquals(2, taskInstances.size());
        }
        // AUTO
        {
            Query query = new Query();
            query.setTaskInstanceStatus(TaskInstanceStatus.SUCCESS);
            query.setTaskScheduleType(TaskScheduleType.AUTO);
            List<TaskInstance> taskInstances = this.instanceService.list(query, 21L, 1, new Paginator());
            Assert.assertEquals(6, taskInstances.size());
        }
    }

    @Test
    @DatabaseSetup
    public void testDeleteByWorkflowInstanceId() throws Exception {
        this.instanceService.deleteByWorkflowInstanceId(21L);
        Assert.assertEquals(0, this.instanceService.listByWorkflowInstanceId(21L).size());
    }

    @Test
    @DatabaseSetup
    public void testListAllDependencyWait() throws Exception {
        List<TaskInstance> taskInstances = this.instanceService.listAllDependencyWait();
        Assert.assertEquals(1, taskInstances.size());
        assertTaskInstance(taskInstances.get(0), Long.valueOf(8), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), null, null, null,
                TaskInstanceStatus.DEPENDENCY_WAIT, 8L, 8L);
    }

    @Test
    @DatabaseSetup
    public void testGetAllUncompleteds() throws Exception {
        List<TaskInstance> taskInstances = this.instanceService.getAllUncompleteds();
        Assert.assertEquals(2, taskInstances.size());
        assertTaskInstance(taskInstances.get(0), Long.valueOf(1), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:38:30"), "2014-11-26 17:38:00",
                "2014-11-26 17:38:10", null, TaskInstanceStatus.RUNNING, 1L, 1L);
        assertTaskInstance(taskInstances.get(1), Long.valueOf(2), sdf.parse("2014-11-26 17:37:00"), sdf.parse("2014-11-26 17:37:00"), "2014-11-26 17:38:00",
                null, null, TaskInstanceStatus.READY, 2L, 2L);
    }

    @Test
    @DatabaseSetup
    public void testUpdateStatus() throws Exception {
        this.instanceService.updateStatus(1L, TaskInstanceStatus.SUCCESS);
        this.instanceService.updateStatus(2L, TaskInstanceStatus.RUNNING);

        Date now = new Date();
        assertTaskInstance(this.instanceService.get(1L), Long.valueOf(1), sdf.parse("2014-11-26 17:37:00"), now, "2014-11-26 17:38:00", "2014-11-26 " +
                "17:38:10", sdf.format(now), TaskInstanceStatus.SUCCESS, 1L, 1L);
        assertTaskInstance(this.instanceService.get(2L), Long.valueOf(2), sdf.parse("2014-11-26 17:37:00"), now, "2014-11-26 17:38:00", sdf.format(now),
                null, TaskInstanceStatus.RUNNING, 2L, 2L);
    }

    @Test
    @DatabaseSetup
    public void testSave() throws Exception {
        TaskInstance instance = new TaskInstance();
        Date now = new Date();
        instance.setScheduleTime(now);
        instance.setStartTime(now);
        instance.setStatus(TaskInstanceStatus.SUCCESS);
        instance.setEndTime(now);
        instance.setTaskId(1L);
        instance.setWorkflowInstanceId(2L);
        this.instanceService.save(instance);

        assertTaskInstance(instance, instance.getId(), now, now, sdf.format(now), sdf.format(now), sdf.format(now), TaskInstanceStatus.SUCCESS, 1L, 2L);
    }

    @Test
    @DatabaseSetup
    public void testExists() throws Exception {
        Assert.assertTrue(this.instanceService.exist(21L, sdf.parse("2014-11-26 17:38:00")));
        Assert.assertFalse(this.instanceService.exist(8L, sdf.parse("2014-11-26 17:38:00")));
        Assert.assertFalse(this.instanceService.exist(100L, sdf.parse("2014-11-26 17:38:00")));
    }

    @Test
    @DatabaseSetup
    public void testListByWorkflowInstanceIdAndUserId() throws Exception {

        List<TaskInstance> listByWorkflowInstanceIdAndUserId = this.instanceService.listByWorkflowInstanceIdAndUserId(1, 1);
        Assert.assertEquals(2, listByWorkflowInstanceIdAndUserId.size());
        Assert.assertEquals(Long.valueOf(1), listByWorkflowInstanceIdAndUserId.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), listByWorkflowInstanceIdAndUserId.get(1).getId());
        Assert.assertTrue(this.instanceService.listByWorkflowInstanceIdAndUserId(1, 4).isEmpty());
        Assert.assertTrue(this.instanceService.listByWorkflowInstanceIdAndUserId(3, 1).isEmpty());
    }

    @Test
    @DatabaseSetup
    public void testListByWorkflowInstanceIdAndUserIdPagination() throws Exception {
        Paginator paginator = new Paginator();
        List<TaskInstance> listByWorkflowInstanceIdAndUserId = this.instanceService.listByWorkflowInstanceIdAndUserId(1, 1, 1, paginator);
        Assert.assertEquals(3, listByWorkflowInstanceIdAndUserId.size());
        Assert.assertEquals(3, paginator.getItems());
        Assert.assertEquals(1, paginator.getPages());
        Assert.assertEquals(Long.valueOf(1), listByWorkflowInstanceIdAndUserId.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), listByWorkflowInstanceIdAndUserId.get(1).getId());
        Assert.assertEquals(Long.valueOf(4), listByWorkflowInstanceIdAndUserId.get(2).getId());
        Assert.assertEquals(0, this.instanceService.listByWorkflowInstanceIdAndUserId(1, 4, 1, paginator).size());
    }

    private void assertTaskInstance(TaskInstance instance, Long id, Date createTime, Date updateTime, String scheduleTime, String startTime, String endTime,
                                    TaskInstanceStatus status, Long taskId, Long workflowInstanceId) {
        Assert.assertEquals(id, instance.getId());
        Assert.assertTrue(Math.abs(createTime.getTime() - instance.getCreateTime().getTime()) < TIME_ACCEPTABLE_DIFF);
        Assert.assertTrue(Math.abs(updateTime.getTime() - instance.getUpdateTime().getTime()) < TIME_ACCEPTABLE_DIFF);
        if (instance.getScheduleTime() != null) {
            Assert.assertEquals(scheduleTime, sdf.format(instance.getScheduleTime()));
        } else {
            Assert.assertNull(instance.getScheduleTime());
        }
        if (instance.getStartTime() != null) {
            Assert.assertEquals(startTime, sdf.format(instance.getStartTime()));
        } else {
            Assert.assertNull(instance.getStartTime());
        }

        if (instance.getEndTime() != null) {
            Assert.assertEquals(endTime, sdf.format(instance.getEndTime()));
        } else {
            Assert.assertNull(instance.getEndTime());
        }
        Assert.assertEquals(status, instance.getStatus());
        Assert.assertEquals(taskId, instance.getTaskId());
        Assert.assertEquals(workflowInstanceId, instance.getWorkflowInstanceId());

    }

}
