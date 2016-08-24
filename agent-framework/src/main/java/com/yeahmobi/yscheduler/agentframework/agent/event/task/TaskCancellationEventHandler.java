package com.yeahmobi.yscheduler.agentframework.agent.event.task;

import java.util.Map;

import com.yeahmobi.yscheduler.agentframework.agent.event.handler.HandlerResult;
import com.yeahmobi.yscheduler.agentframework.agent.event.handler.TaskExecutionEventHandler;

/**
 * @author Leo.Liang
 */
public class TaskCancellationEventHandler extends TaskExecutionEventHandler {

    public static final String EVENT_TYPE = "TASK_CANCEL";

    public void onEvent(Map<String, String> params, HandlerResult handlerResult) {
        cancel(params, handlerResult);
    }
}
