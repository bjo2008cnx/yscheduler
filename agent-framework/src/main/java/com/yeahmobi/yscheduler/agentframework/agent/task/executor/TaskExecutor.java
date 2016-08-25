package com.yeahmobi.yscheduler.agentframework.agent.task.executor;

import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransaction;

/**
 * @author Leo.Liang
 */
public interface TaskExecutor<T extends AgentTask> {

    public Integer execute(TaskTransaction<T> taskTransaction) throws Exception;

    public Integer recover(TaskTransaction<T> taskTransaction) throws Exception;

    public void cancel(TaskTransaction<T> taskTransaction) throws Exception;

}
