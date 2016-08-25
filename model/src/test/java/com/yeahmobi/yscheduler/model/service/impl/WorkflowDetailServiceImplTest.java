package com.yeahmobi.yscheduler.model.service.impl;

import com.yeahmobi.yscheduler.model.WorkflowDetail;
import com.yeahmobi.yscheduler.model.service.WorkflowDetailService;
import com.yeahmobi.yscheduler.model.type.DependingStatus;
import com.yeahmobi.yunit.DbUnitTestExecutionListener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Ryan Sun
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-test.xml"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class})
public class WorkflowDetailServiceImplTest {

    @Autowired
    private WorkflowDetailService workflowDetailService;

    @Test
    public void testSave() {
        long workflowId = 1l;
        List<WorkflowDetail> details = new ArrayList<WorkflowDetail>();
        List<List<Long>> dependencyList = new ArrayList<List<Long>>();
        details.add(buildWorkflowDetail(1l));
        details.add(buildWorkflowDetail(2l));
        List<Long> dependencies = new ArrayList<Long>();
        dependencies.add(1l);
        dependencyList.add(new ArrayList<Long>());
        dependencyList.add(dependencies);
        this.workflowDetailService.save(workflowId, details, dependencyList);
        List<WorkflowDetail> actuals = this.workflowDetailService.list(workflowId);
        assertWorkflowDetailArrayEquals(details, actuals);
        Assert.assertArrayEquals(dependencies.toArray(), this.workflowDetailService.listDependencyTaskIds(1l, 2l).toArray());

    }

    @Test
    public void testGet() {
        long workflowId = 1l;
        List<WorkflowDetail> details = new ArrayList<WorkflowDetail>();
        List<List<Long>> dependencyList = new ArrayList<List<Long>>();
        details.add(buildWorkflowDetail(1l));
        details.add(buildWorkflowDetail(2l));
        List<Long> dependencies = new ArrayList<Long>();
        dependencies.add(1l);
        dependencyList.add(new ArrayList<Long>());
        dependencyList.add(dependencies);
        this.workflowDetailService.save(workflowId, details, dependencyList);

        WorkflowDetail wd = this.workflowDetailService.get(1, 2);
        Assert.assertNotNull(wd);
        Assert.assertEquals(Long.valueOf(2), wd.getId());
        Assert.assertEquals(Integer.valueOf(100), wd.getDelay());

        WorkflowDetail wd1 = this.workflowDetailService.get(1, 3);
        Assert.assertNull(wd1);

        WorkflowDetail wd2 = this.workflowDetailService.get(2, 2);
        Assert.assertNull(wd2);
    }

    private void assertWorkflowDetailArrayEquals(List<WorkflowDetail> details, List<WorkflowDetail> actuals) {
        for (int i = 0; i < details.size(); i++) {
            WorkflowDetail detail = details.get(i);
            WorkflowDetail actual = actuals.get(i);
            Assert.assertEquals(detail.getId(), actual.getId());
            Assert.assertEquals(detail.getUpdateTime(), actual.getUpdateTime());

        }
    }

    private WorkflowDetail buildWorkflowDetail(long taskId) {
        WorkflowDetail result = new WorkflowDetail();
        result.setRetryTimes(0);
        result.setTaskId(taskId);
        result.setTimeout(1);
        result.setWorkflowId(1l);
        result.setDelay(100);
        result.setLastStatusDependency(DependingStatus.SUCCESS);
        return result;
    }

}
