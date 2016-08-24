package com.yeahmobi.yscheduler.agentframework.agent.servlet;

import com.alibaba.fastjson.JSON;
import com.yeahmobi.yscheduler.agentframework.AgentRequest;
import com.yeahmobi.yscheduler.agentframework.AgentResponse;
import com.yeahmobi.yscheduler.agentframework.AgentResponseCode;
import com.yeahmobi.yscheduler.agentframework.agent.event.handler.HandlerResult;
import com.yeahmobi.yscheduler.agentframework.agent.event.handler.PingEventHandler;
import com.yeahmobi.yscheduler.agentframework.agent.event.handler.EventHandler;
import com.yeahmobi.yscheduler.agentframework.agent.event.mapper.DefaultEventMapper;
import com.yeahmobi.yscheduler.agentframework.agent.event.mapper.EventMapper;
import com.yeahmobi.yscheduler.agentframework.agent.event.mapper.EventMapperFactory;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 任务执行通过http servlet方式？
 *
 * @author Leo.Liang
 */
public class AgentDispatchServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AgentDispatchServlet.class);

    private static final String SERVLET_CONFIG_EVENT_MAPPER_FACTORY = "eventMapperFactory";
    private static final String EVENT_TYPE_PING = "ping";

    private EventMapper eventMapper;

    @Override
    public void init(ServletConfig config) throws ServletException {
        initEventMapper(config);

        super.init(config);
    }

    private void initEventMapper(ServletConfig config) throws ServletException {
        EventMapperFactory eventMapperFactory = getEventMapperFactory(config);

        if (eventMapperFactory == null) {
            DefaultEventMapper em = new DefaultEventMapper();
            em.init();
            this.eventMapper = em;
        } else {
            this.eventMapper = eventMapperFactory.getEventMapper(config.getServletContext());
        }

        registerCommonEventHandler();

    }

    private void registerCommonEventHandler() {
        this.eventMapper.add(EVENT_TYPE_PING, new PingEventHandler());
    }

    @SuppressWarnings("unchecked")
    private EventMapperFactory getEventMapperFactory(ServletConfig config) throws ServletException {
        String eventMapperFactoryClassString = config.getInitParameter(SERVLET_CONFIG_EVENT_MAPPER_FACTORY);
        if (StringUtils.isNotBlank(eventMapperFactoryClassString)) {
            try {
                Class<EventMapperFactory> eventMapperFactoryClass = (Class<EventMapperFactory>) Class.forName(eventMapperFactoryClassString);
                return ConstructorUtils.invokeConstructor(eventMapperFactoryClass, null);
            } catch (Throwable e) {
                log.error("Fail to init EventMapperFactory({}).", eventMapperFactoryClassString);
                throw new ServletException(String.format("Fail to init EventMapperFactory(%s).", eventMapperFactoryClassString));
            }
        }
        return null;
    }

    /**
     * @param eventMapper the eventMapper to set
     */
    public void setEventMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        AgentResponse<Object> agentRes = new AgentResponse<Object>();

        String requestUri = req.getServletPath();
        if (!validateReqeustPath(requestUri)) {
            agentRes.setResponseCode(AgentResponseCode.REQUEST_URI_INVALID);
            log.error("Request uri invalid(uri={}).", requestUri);
        } else {
            try {
                AgentRequest agentReq = AgentRequest.valueOf(req);
                EventHandler handler = this.eventMapper.findHandler(agentReq.getEventType());

                log.info("Reqeust access...(EventType={}, EventHandler={})", agentReq.getEventType(), handler == null ? "null" : handler.getClass().getName());

                if (handler == null) {
                    agentRes.setResponseCode(AgentResponseCode.EVENT_NOT_SUPPORTED);
                    log.error("Event type not supported(eventType={}).", agentReq.getEventType());
                } else {
                    try {
                        HandlerResult handlerResult = new HandlerResult();

                        handler.onEvent(agentReq.getParams(), handlerResult);

                        if (!handlerResult.isSuccess()) {
                            log.error("Event handler error(errMsg={}).", handlerResult.getErrorMsg());
                            agentRes.setResponseCode(AgentResponseCode.EVENT_HANDLER_ERROR);
                            agentRes.setErrorMsg(handlerResult.getErrorMsg());
                            agentRes.setThrowable(handlerResult.getThrowable());
                        } else {
                            agentRes.setResponseCode(AgentResponseCode.SUCCESS);
                            if (handlerResult.getResult() != null) {
                                agentRes.setResponseData(handlerResult.getResult());
                            }
                        }
                    } catch (Throwable e) {
                        log.error("Event handler error", e);
                        agentRes.setResponseCode(AgentResponseCode.EVENT_HANDLER_ERROR);
                        agentRes.setErrorMsg(e.getMessage());
                        agentRes.setThrowable(e);
                    }
                }
            } catch (Throwable e) {
                log.error("Unknown error", e);
                agentRes.setResponseCode(AgentResponseCode.UNKNOWN_ERROR);
                agentRes.setErrorMsg(e.getMessage());
                agentRes.setThrowable(e);
            }
        }

        writeResponse(resp, agentRes);

        resp.flushBuffer();
    }

    private boolean validateReqeustPath(String requestUri) {
        if ("/".equals(requestUri)) {
            return true;
        }

        return false;
    }

    /**
     * @param resp
     * @param agentRes
     * @throws IOException
     */
    private void writeResponse(HttpServletResponse resp, AgentResponse agentRes) throws IOException {
        resp.getOutputStream().write(JSON.toJSONBytes(agentRes));
    }
}
