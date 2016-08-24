package com.yeahmobi.yscheduler.agentframework.agent.task.agenttask;

import com.yeahmobi.yscheduler.agentframework.agent.event.task.CalloutTaskExecutor;

import java.util.Map;

/**
 * 异步任务
 */
public class CalloutAgentTask extends AbstractAgentTask {

    public CalloutAgentTask(String eventType, Map<String, String> params) {
        super(eventType, params);
    }

    private CalloutTaskExecutor executor = new CalloutTaskExecutor();

    public CalloutTaskExecutor getTaskExecutor() {
        return this.executor;
    }

}
