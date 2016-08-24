package com.yeahmobi.yscheduler.agentframework.agent.task.executor;

import com.yeahmobi.yscheduler.agentframework.agent.task.TaskLog;
import com.yeahmobi.yscheduler.agentframework.agent.task.TaskStatus;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.exception.TaskNotFoundException;
import com.yeahmobi.yscheduler.agentframework.exception.TaskSubmitException;

/**
 * @author Leo.Liang
 */
public interface TaskExecutionContainer {

    public long submit(AgentTask task) throws TaskSubmitException;

    public TaskStatus checkStatus(long transactionId) throws TaskNotFoundException;

    public TaskLog getLog(long transactionId, long offset, int length) throws TaskNotFoundException;

    public void cancel(long transactionId) throws TaskNotFoundException;
}
