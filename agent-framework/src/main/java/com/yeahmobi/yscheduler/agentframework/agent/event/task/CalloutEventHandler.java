package com.yeahmobi.yscheduler.agentframework.agent.event.task;

import java.util.Map;

import com.yeahmobi.yscheduler.agentframework.agent.event.handler.TaskSubmitionEventHandler;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.CalloutAgentTask;

/**
 * @author atell.wu
 */
public class CalloutEventHandler extends TaskSubmitionEventHandler {

    public static final String EVENT_TYPE = "TASK_HTTP_CALLOUT";

    @Override
    public AgentTask getTask(Map<String, String> params) {
        CalloutAgentTask agentTask = new CalloutAgentTask(EVENT_TYPE, params);

        return agentTask;
    }

}
