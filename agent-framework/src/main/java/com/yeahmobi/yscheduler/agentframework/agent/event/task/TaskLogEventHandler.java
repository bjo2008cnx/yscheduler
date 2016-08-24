package com.yeahmobi.yscheduler.agentframework.agent.event.task;

import java.util.Map;

import com.yeahmobi.yscheduler.agentframework.agent.event.handler.HandlerResult;
import com.yeahmobi.yscheduler.agentframework.agent.event.handler.TaskExecutionEventHandler;

/**
 * @author Leo.Liang
 */
public class TaskLogEventHandler extends TaskExecutionEventHandler {

    public static final String EVENT_TYPE = "TASK_LOG";

    public void onEvent(Map<String, String> params, HandlerResult handlerResult) {
        getLog(params, handlerResult);
    }
}
