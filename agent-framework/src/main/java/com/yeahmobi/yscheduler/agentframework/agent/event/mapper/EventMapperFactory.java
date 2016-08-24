package com.yeahmobi.yscheduler.agentframework.agent.event.mapper;

import com.yeahmobi.yscheduler.agentframework.agent.event.mapper.EventMapper;

import javax.servlet.ServletContext;

/**
 * @author Leo.Liang
 */
public interface EventMapperFactory {

    public EventMapper getEventMapper(ServletContext servletContext);
}
