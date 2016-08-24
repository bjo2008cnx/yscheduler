package com.yeahmobi.yscheduler.agentframework.agent.task.transaction;

import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.exception.TaskNotFoundException;
import com.yeahmobi.yscheduler.agentframework.exception.TaskTransactionCreationException;

import java.util.List;

/**
 * @author Leo.Liang
 */
public interface TaskTransactionManager {

    public TaskTransaction getTransaction(long transactionId) throws TaskNotFoundException;

    public TaskTransaction getTransaction(long transactionId, AgentTask task) throws TaskTransactionCreationException,
                                                                             TaskNotFoundException;

    public TaskTransaction createTransaction(AgentTask task) throws TaskTransactionCreationException;

    public List<TaskTransaction> getAllTransaction() throws TaskNotFoundException;
}
