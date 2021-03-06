/**
 *
 */
package com.yeahmobi.yscheduler.agentframework.agent.event.mapper;

import com.yeahmobi.yscheduler.agentframework.agent.event.handler.EventHandler;

/**
 * 事件类型与事件处理器的映射
 *
 * @author Leo.Liang
 */
public interface EventMapper {

    public EventHandler findHandler(String eventType);

    public void add(String eventType, EventHandler eventHandler);
}
