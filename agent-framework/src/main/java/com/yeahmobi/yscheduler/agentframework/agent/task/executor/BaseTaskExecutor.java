package com.yeahmobi.yscheduler.agentframework.agent.task.executor;

import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransaction;

/**
 * @author Leo.Liang
 */
public abstract class BaseTaskExecutor<T extends AgentTask> implements TaskExecutor<T> {

    public Integer recover(TaskTransaction<T> taskTransaction) throws Exception {
        return execute(taskTransaction);
    }

}
