package com.yeahmobi.yscheduler.agentframework.agent.event.handler;

import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.ShellAgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.executor.ShellTaskExecutor;

import java.util.Map;

/**
 * @author Leo.Liang
 */
public class ShellTaskSubmitionEventHandler extends TaskSubmitionEventHandler {

    public static final String EVENT_TYPE = "SHELL_TASK_SUBMIT";

    private ShellTaskExecutor  taskExecutor;

    public void setShellTaskExecutor(ShellTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public AgentTask getTask(Map<String, String> params) {
        return new ShellAgentTask(EVENT_TYPE, params, this.taskExecutor);
    }

}
