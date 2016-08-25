package com.yeahmobi.yscheduler.condition;

import com.yeahmobi.yscheduler.model.*;

import java.util.List;

public class ConditionContext {

    private Workflow workflow;

    private WorkflowInstance workflowInstance;

    private WorkflowDetail workflowDetail;

    private Task task;

    private TaskInstance taskInstance;

    private List<TaskInstance> dependencyTaskInstances;

    public ConditionContext(Workflow workflow, WorkflowInstance workflowInstance, WorkflowDetail workflowDetail, Task task, TaskInstance taskInstance,
                            List<TaskInstance> dependencyTaskInstances) {
        super();
        this.workflow = workflow;
        this.workflowInstance = workflowInstance;
        this.workflowDetail = workflowDetail;
        this.task = task;
        this.taskInstance = taskInstance;
        this.dependencyTaskInstances = dependencyTaskInstances;
    }

    public Workflow getWorkflow() {
        return this.workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public WorkflowInstance getWorkflowInstance() {
        return this.workflowInstance;
    }

    public void setWorkflowInstance(WorkflowInstance workflowInstance) {
        this.workflowInstance = workflowInstance;
    }

    public WorkflowDetail getWorkflowDetail() {
        return this.workflowDetail;
    }

    public void setWorkflowDetail(WorkflowDetail workflowDetail) {
        this.workflowDetail = workflowDetail;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public TaskInstance getTaskInstance() {
        return this.taskInstance;
    }

    public void setTaskInstance(TaskInstance taskInstance) {
        this.taskInstance = taskInstance;
    }

    public List<TaskInstance> getDependencyTaskInstances() {
        return this.dependencyTaskInstances;
    }

    public void setDependencyTaskInstances(List<TaskInstance> dependencyTaskInstances) {
        this.dependencyTaskInstances = dependencyTaskInstances;
    }

}
