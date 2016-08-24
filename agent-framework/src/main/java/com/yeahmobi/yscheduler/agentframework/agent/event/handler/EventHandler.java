/**
 *
 */
package com.yeahmobi.yscheduler.agentframework.agent.event.handler;

import java.util.Map;

/**
 * @author Leo.Liang
 */
public interface EventHandler {

    /**
     * @param params
     * @param handlerResult
     */
    public void onEvent(Map<String, String> params, HandlerResult handlerResult);

}
