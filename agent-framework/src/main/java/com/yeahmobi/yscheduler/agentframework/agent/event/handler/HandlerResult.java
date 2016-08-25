/**
 *
 */
package com.yeahmobi.yscheduler.agentframework.agent.event.handler;

import lombok.Data;

/**
 * @author Leo.Liang
 */
@Data
public class HandlerResult {

    private boolean success = true;
    private String errorMsg;
    private Throwable throwable;
    private Object result;

}
