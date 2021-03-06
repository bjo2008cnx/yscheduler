package com.yeahmobi.yscheduler.agentframework.agent.event.handler;

import java.util.Map;

/**
 * @author Leo.Liang
 */
public class PingEventHandler implements EventHandler {

    public static final String EVENT_TYPE = "PING";

    public void onEvent(Map<String, String> params, HandlerResult handlerResult) {
        handlerResult.setSuccess(true);
    }

}
