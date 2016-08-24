package com.yeahmobi.yscheduler.agentframework.agent.task.executor;

import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.FileBasedTaskTransactionManager;
import com.yeahmobi.yscheduler.agentframework.exception.TaskTransactionManagerInitializeFailException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Leo.Liang
 */
public class TaskExecutionContainerFactory {

    private static volatile ConcurrentMap<String, TaskExecutionContainer> defaultTaskExecutionContainers = new ConcurrentHashMap<String, TaskExecutionContainer>();

    public static TaskExecutionContainer getDefaultTaskExecutionContainer(String transactionDataBaseDir)
                                                                                                        throws TaskTransactionManagerInitializeFailException {
        if (!defaultTaskExecutionContainers.containsKey(transactionDataBaseDir)) {
            synchronized (defaultTaskExecutionContainers) {
                if (!defaultTaskExecutionContainers.containsKey(transactionDataBaseDir)) {
                    DefaultTaskExecutionContainer tec = new DefaultTaskExecutionContainer();
                    FileBasedTaskTransactionManager transactionManager = new FileBasedTaskTransactionManager();
                    transactionManager.init();
                    transactionManager.setBaseDir(transactionDataBaseDir);
                    tec.setTaskTransactionManager(transactionManager);
                    defaultTaskExecutionContainers.put(transactionDataBaseDir, tec);
                }
            }
        }

        return defaultTaskExecutionContainers.get(transactionDataBaseDir);
    }
}
