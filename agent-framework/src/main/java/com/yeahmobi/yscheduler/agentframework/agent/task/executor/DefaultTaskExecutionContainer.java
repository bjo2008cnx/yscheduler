package com.yeahmobi.yscheduler.agentframework.agent.task.executor;

import com.yeahmobi.yscheduler.agentframework.agent.event.handler.EventHandler;
import com.yeahmobi.yscheduler.agentframework.agent.event.mapper.EventMapper;
import com.yeahmobi.yscheduler.agentframework.agent.event.handler.TaskSubmitionEventHandler;
import com.yeahmobi.yscheduler.agentframework.agent.task.*;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransaction;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransaction.Meta;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransactionManager;
import com.yeahmobi.yscheduler.agentframework.exception.TaskNotFoundException;
import com.yeahmobi.yscheduler.agentframework.exception.TaskSubmitException;
import com.yeahmobi.yscheduler.agentframework.exception.TaskTransactionCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Leo.Liang
 */
public class DefaultTaskExecutionContainer implements TaskExecutionContainer {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskExecutionContainer.class);

    private ExecutorService workerPool = Executors.newCachedThreadPool();
    private final ConcurrentMap<Long, TaskTransaction> runningTransactions = new ConcurrentHashMap<Long, TaskTransaction>();

    private TaskTransactionManager taskTransactionManager;
    private EventMapper eventMapper;

    public void setTaskTransactionManager(TaskTransactionManager transactionManager) {
        this.taskTransactionManager = transactionManager;
    }

    public void setEventMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public void init() throws TaskNotFoundException, IOException, TaskSubmitException, TaskTransactionCreationException {
        // 扫描未结束的tx，按照type，构建 AgentTask，submit
        List<TaskTransaction> allTransaction = this.taskTransactionManager.getAllTransaction();
        if ((allTransaction != null) && (allTransaction.size() > 0)) {
            for (TaskTransaction tx : allTransaction) {
                if (!tx.getMeta().getStatus().isCompleted()) {
                    try {
                        log.info("Task(transactionId=" + tx.getId() + ") is continued because status is " + tx.getMeta().getStatus());

                        AgentTask task = loadAgentTask(tx.getMeta());

                        TaskTransaction transaction = this.taskTransactionManager.getTransaction(tx.getId(), task);
                        execTransaction(task, transaction);

                    } catch (Exception e) {
                        log.error(String.format("Error when continuing transaction(txId=%s), skip this transaction.", tx.getId()), e);
                        tx.error(String.format("Error when continuing: %s", e.getMessage()), e);
                    }
                }
            }
        }

    }

    private AgentTask loadAgentTask(Meta meta) {
        // 根据 type 去创建agentTask，这个和handler创建task是一个处理方法。
        String eventType = meta.getEventType();
        EventHandler handler = this.eventMapper.findHandler(eventType);

        if (handler == null) {
            throw new IllegalArgumentException("Handler of eventType(" + eventType + ") not found");
        }

        if (!(handler instanceof TaskSubmitionEventHandler)) {
            throw new IllegalArgumentException("Handler of eventType(" + eventType + ") is not instance of TaskSubmitionEventHandler, can not be submit.");
        }

        return ((TaskSubmitionEventHandler) handler).getTask(meta.getTaskParams());
    }

    public long submit(AgentTask task) throws TaskSubmitException {
        try {
            TaskTransaction transaction = this.taskTransactionManager.createTransaction(task);
            return execTransaction(task, transaction);
        } catch (TaskTransactionCreationException e) {
            log.error(String.format("Fail to submit task. (AgentTask's type=%s)", task.getClass().getName()), e);
            throw new TaskSubmitException(e);
        }
    }

    private long execTransaction(AgentTask task, final TaskTransaction transaction) throws TaskSubmitException {
        try {
            final long txId = transaction.getId();
            this.runningTransactions.put(txId, transaction);

            this.workerPool.submit(new Callable<Void>() {

                public Void call() {
                    try {
                        transaction.execute();

                        // if (transaction.getMeta().getStatus() == TaskTransactionStatus.SUCCESS) {
                        // log.info("Agent task execute successfully.(transactionId={})", transaction.getId());
                        // } else if (transaction.getMeta().getStatus() == TaskTransactionStatus.FAIL) {
                        // log.info("Fail to execute agent task.(transactionId={})", transaction.getId());
                        // } else if (transaction.getMeta().getStatus() == TaskTransactionStatus.CANCEL) {
                        // log.info("Agent task cancelled.(transactionId={})", transaction.getId());
                        // } else if (transaction.getMeta().getStatus() ==
                        // TaskTransactionStatus.COMPLETE_WITH_UNKNOWN_STATUS) {
                        // log.info("Agent task completed with unknown status.(transactionId={})", transaction.getId());
                        // }
                    } finally {
                        DefaultTaskExecutionContainer.this.runningTransactions.remove(txId);
                    }

                    return null;
                }
            });

            return txId;

        } catch (Throwable e) {
            log.error(String.format("Fail to submit task. (AgentTask's type=%s)", task.getClass().getName()), e);
            throw new TaskSubmitException(e);
        }
    }

    public TaskStatus checkStatus(long transactionId) throws TaskNotFoundException {
        TaskTransaction transaction = findTransaction(transactionId);
        return new TaskStatus(transaction.getMeta().getStatus(), transaction.getMeta().getDuration(), transaction.getMeta().getReturnValue());
    }

    public TaskLog getLog(long transactionId, long offset, int length) throws TaskNotFoundException {
        return findTransaction(transactionId).getLog(offset, length);
    }

    private TaskTransaction findTransaction(long transactionId) throws TaskNotFoundException {
        TaskTransaction tx = this.runningTransactions.get(transactionId);
        if (tx != null) {
            return tx;
        } else {
            return this.taskTransactionManager.getTransaction(transactionId);
        }
    }

    public void cancel(long transactionId) throws TaskNotFoundException {
        TaskTransaction tx = this.runningTransactions.get(transactionId);
        if (tx == null) {
            throw new TaskNotFoundException(String.format("Task with transaction id {%s} not found or not running", String.valueOf(transactionId)));
        } else {
            tx.cancel();
            this.runningTransactions.remove(transactionId);
        }
    }
}
