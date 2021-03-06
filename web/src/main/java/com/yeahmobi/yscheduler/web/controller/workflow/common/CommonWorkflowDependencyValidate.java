package com.yeahmobi.yscheduler.web.controller.workflow.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.jsoup.helper.Validate;

import com.yeahmobi.yscheduler.model.service.TaskService;
import com.yeahmobi.yscheduler.model.service.UserService;
import com.yeahmobi.yscheduler.web.vo.WorkflowDetailVO;

/**
 * @author Ryan Sun
 */
public class CommonWorkflowDependencyValidate {

    private List<WorkflowDetailVO> detailVos   = new ArrayList<WorkflowDetailVO>();

    private Map<Long, List<Long>>  tasks       = new HashMap<Long, List<Long>>();

    private Map<Long, List<Long>>  revertTasks = new HashMap<Long, List<Long>>();

    private Long                   endTask;

    private TaskService            taskService;

    private UserService            userService;

    private void buildTasks() {
        for (WorkflowDetailVO detailVo : this.detailVos) {
            Long taskId = detailVo.getWorkflowDetail().getTaskId();
            String name = detailVo.getTaskName();
            if (taskId == null) {
                Validate.fail("添加任务不存在！");
            }
            if (this.tasks.containsKey(taskId)) {
                Validate.fail("重复添加任务" + name + "!");
            }

            List<Long> dependencyTaskIds = new ArrayList<Long>(detailVo.getDependencies());
            this.tasks.put(taskId, dependencyTaskIds);

            for (Long dependencyTaskId : dependencyTaskIds) {
                if (taskId.equals(dependencyTaskId)) {
                    Validate.fail("任务" + name + "依赖了它自己！");
                }
            }
        }
    }

    private void revert() {
        for (Entry<Long, List<Long>> entry : this.tasks.entrySet()) {
            Long taskId = entry.getKey();
            List<Long> dependencies = entry.getValue();
            for (Long dependency : dependencies) {
                List<Long> revertValue = this.revertTasks.get(dependency);
                if (revertValue == null) {
                    revertValue = new ArrayList<Long>();
                    this.revertTasks.put(dependency, revertValue);
                }
                revertValue.add(taskId);
            }
        }
    }

    private void buildEndTasks() {
        List<Long> endTasks = new ArrayList<Long>();
        for (Long taskId : this.revertTasks.keySet()) {
            endTasks.add(taskId);
        }
        for (WorkflowDetailVO detail : this.detailVos) {
            if (!detail.getDependencies().isEmpty()) {
                endTasks.remove(detail.getWorkflowDetail().getTaskId());
            }
        }
        if (endTasks.size() == 1) {
            this.endTask = endTasks.get(0);
        } else if (endTasks.size() == 0) {
            Validate.fail("工作流中存在循环依赖！");
        } else if (endTasks.size() > 1) {
            Validate.fail("工作流中存在多个起点！");
        }
    }

    private void dependOnTaskNotInThisWorkflow() {
        for (WorkflowDetailVO detail : this.detailVos) {
            List<Long> taskIds = detail.getDependencies();
            for (Long taskId : taskIds)
                if ((taskId != null) && !this.tasks.containsKey(taskId)) {
                    String name = detail.getTaskName();
                    Long currentTaskId = detail.getWorkflowDetail().getTaskId();
                    String owner = this.userService.get(this.taskService.get(currentTaskId).getOwner()).getName();
                    if (detail.getNeedValidate()) {
                        Validate.fail("任务" + name + "依赖了没有配置在工作流中的任务");
                    } else {
                        String dependencyName = this.taskService.get(taskId).getName();
                        Validate.fail(String.format("%s被%s的%s依赖，所以不能被删除！", dependencyName, owner, name));
                    }

                }
        }
    }

    private boolean circularDependency() {
        Stack<Long> stack = new Stack<Long>();
        stack.push(this.endTask);
        boolean result = cicular(stack);
        if (!result) {
            result = this.tasks.size() != 0;
        }
        return result;
    }

    private boolean cicular(Stack<Long> stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Long item = stack.peek();
        List<Long> dependencyTasks = this.revertTasks.get(item);
        if ((dependencyTasks == null) || (dependencyTasks.size() == 0)) {
            this.tasks.remove(stack.pop());
            return cicular(stack);
        } else {
            Long dependencyTask = dependencyTasks.remove(0);
            if (stack.contains(dependencyTask)) {
                return true;
            }
            stack.push(dependencyTask);
            // TODO 改成非递归
            return cicular(stack);
        }
    }

    void validate() {
        buildTasks();
        dependOnTaskNotInThisWorkflow();
        revert();
        buildEndTasks();
        Validate.isFalse(circularDependency(), "工作流中存在循环依赖！");
    }

    CommonWorkflowDependencyValidate(List<WorkflowDetailVO> detailVos, TaskService taskService, UserService userService) {
        this.detailVos = detailVos;
        this.taskService = taskService;
        this.userService = userService;
    }
}
