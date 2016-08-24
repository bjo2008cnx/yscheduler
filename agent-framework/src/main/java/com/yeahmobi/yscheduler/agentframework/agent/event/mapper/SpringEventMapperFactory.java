package com.yeahmobi.yscheduler.agentframework.agent.event.mapper;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Leo.Liang
 */
public class SpringEventMapperFactory implements EventMapperFactory {

    public EventMapper getEventMapper(ServletContext servletContext) {
        return WebApplicationContextUtils.getWebApplicationContext(servletContext).getBean(SpringEventMapper.class);
    }

}
