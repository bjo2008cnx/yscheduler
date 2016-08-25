package com.yeahmobi.yscheduler.executor;

import com.yeahmobi.yscheduler.model.Attempt;
import com.yeahmobi.yscheduler.model.Task;
import com.yeahmobi.yscheduler.model.TaskInstance;
import com.yeahmobi.yscheduler.model.service.AttemptService;
import com.yeahmobi.yscheduler.model.service.TaskInstanceService;
import com.yeahmobi.yscheduler.model.service.TaskService;
import com.yeahmobi.yscheduler.model.type.AttemptStatus;
import com.yeahmobi.yscheduler.model.type.TaskInstanceStatus;
import com.yeahmobi.yscheduler.notice.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DefaultTaskInstanceExecutor implements TaskInstanceExecutor {

    private static final Logger         LOGGER              = LoggerFactory.getLogger(DefaultTaskInstanceExecutor.class);

    private Map<Long, Pair>             instanceMap         = new ConcurrentHashMap<Long, Pair>();

    private ConcurrentSkipListSet<Long> instancesToBeCancel = new ConcurrentSkipListSet<Long>();

    @Autowired
    private TaskService                 taskService;

    @Autowired
    private TaskInstanceService         instanceService;

    @Autowired
    private AttemptExecutor             attemptExecutor;

    @Autowired
    private AttemptService              attemptService;

    @Autowired
    private NoticeService               noticeService;

    private AtomicBoolean               closed              = new AtomicBoolean(false);

    private ControllerThread            controllerThread;

    @PostConstruct
    public void init() {
        // 加载RUNNING和READY的instance
        List<TaskInstance> instanceList = this.instanceService.getAllUncompleteds();
        for (TaskInstance instance : instanceList) {
            putPair(instance);
        }

        // 启动后台执行线程
        this.controllerThread = new ControllerThread();
        this.controllerThread.setName("instance-controller");
        this.controllerThread.setDaemon(true);
        this.controllerThread.start();
    }

    @PreDestroy
    public void close() {
        if (this.closed.compareAndSet(false, true)) {
            this.controllerThread.interrupt();
        }
    }

    private void putPair(TaskInstance instance) {
        if (instance.getStatus() == TaskInstanceStatus.READY) {
            this.attemptService.archiveExistsAttempts(instance.getId());
        }

        if (!TaskInstanceStatus.RUNNING.equals(instance.getStatus())) {
            this.instanceService.updateStatus(instance.getId(), TaskInstanceStatus.RUNNING);
        }
        Long taskId = instance.getTaskId();
        Task task = this.taskService.get(taskId);
        if (task != null) {
            Pair pair = new Pair(task, instance);
            this.instanceMap.put(instance.getId(), pair);
        }
    }

    public void submit(final TaskInstance instance) {
        // 提交给map
        putPair(instance);
    }

    /**
     * 负责：<br>
     * 1. 更新instance的状态<br>
     * 2. 在attempt失败时决定是否重试
     */
    private class ControllerThread extends Thread {

        @Override
        public void run() {

            while (!DefaultTaskInstanceExecutor.this.closed.get()) {
                Iterator<Entry<Long, Pair>> iterator = DefaultTaskInstanceExecutor.this.instanceMap.entrySet().iterator();
                while (iterator.hasNext() && !DefaultTaskInstanceExecutor.this.closed.get()) {
                    Entry<Long, Pair> entry = iterator.next();
                    try {
                        long instanceId = entry.getKey();
                        Pair pair = entry.getValue();
                        Task task = pair.task;
                        TaskInstance instance = pair.instance;

                        // 是否取消
                        boolean isCancelled = isCancelled(instanceId);

                        // 若有正在运行的attempt，则检查是否有cancel和超时的操作。(此时instance状态仍是running，同时不需要重试，故不做任何事情)
                        if (DefaultTaskInstanceExecutor.this.attemptExecutor.isRunning(instanceId)) {
                            if (isCancelled) {
                                // 如果是待取消的task，则取消运行
                                DefaultTaskInstanceExecutor.this.attemptExecutor.cancel(instanceId);
                            } else {
                                // 超时只是报警，无做其他处理，故在loop里每次检查一下即可
                                checkTimeout(pair);
                            }
                            continue;
                        }

                        // 判断该instance的最新attempt的状态(访问db)
                        // 1. 如果是成功
                        // -- 更新instance状态为success(更新db)，从map中移除该instance
                        // 2. 如果是失败或没有任何attempt
                        // -- 则判断该instance的attempt数量(访问db)
                        // ---(1)若未达到retryCount
                        // ----- 创建attempt，提交给AttemptExecutor
                        // ---(2)若达到retryCount
                        // ----- 更新instance状态为failed(更新db)，从map中移除该instance
                        Attempt attempt = DefaultTaskInstanceExecutor.this.attemptService.getLastOne(instanceId);
                        if ((attempt == null) || (attempt.getStatus() == AttemptStatus.FAILED)
                            || (attempt.getStatus() == AttemptStatus.COMPLETE_WITH_UNKNOWN_STATUS)) {
                            // 失败时，尝试重试的逻辑
                            // -- 重试次数未满
                            // --- 未取消，则重试
                            // --- 已取消，则cancelled
                            // -- 重试次数已满，则failed
                            int count = DefaultTaskInstanceExecutor.this.attemptService.countActive(instanceId);
                            int retryTimes = task.getRetryTimes();
                            if (count < (retryTimes + 1)) {
                                if (!isCancelled) {
                                    Attempt attempt0 = new Attempt();
                                    attempt0.setStatus(AttemptStatus.RUNNING);
                                    attempt0.setAgentId(task.getAgentId());
                                    attempt0.setTaskId(task.getId());
                                    attempt0.setInstanceId(instance.getId());
                                    attempt0.setStartTime(new Date());
                                    attempt0.setActive(true);
                                    DefaultTaskInstanceExecutor.this.attemptExecutor.submit(attempt0);
                                } else {
                                    endWithCancelled(instanceId);
                                }
                            } else {
                                endWithFailed(instanceId);
                            }
                        } else if (attempt.getStatus() == AttemptStatus.SUCCESS) {
                            endWithSuccess(instanceId);
                            notifySuccessIfTimeout(pair);
                        } else if (attempt.getStatus() == AttemptStatus.CANCELLED) {
                            endWithCancelled(instanceId);
                        }
                    } catch (RuntimeException e) {
                        // log and continue
                        LOGGER.error("Error when deel with instance, but controller thread will still go on.", e);
                    } finally {
                        try {
                            TimeUnit.MILLISECONDS.sleep(20);
                        } catch (InterruptedException e) {
                            // TODO ignore
                        }
                    }
                }

                // 清理instancesToBeCancel中，已经无用的instanceId
                Iterator<Long> it = DefaultTaskInstanceExecutor.this.instancesToBeCancel.iterator();
                while (it.hasNext()) {
                    Long id = it.next();
                    if (!DefaultTaskInstanceExecutor.this.instanceMap.containsKey(id)) {
                        it.remove();
                    }
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }

        // 取消只做一次，以免无限的取消
        private boolean isCancelled(long instanceId) {
            boolean isCancelled = DefaultTaskInstanceExecutor.this.instancesToBeCancel.remove(instanceId);
            return isCancelled;
        }

    }

    private void endWithSuccess(long instanceId) {
        DefaultTaskInstanceExecutor.this.instanceService.updateStatus(instanceId, TaskInstanceStatus.SUCCESS);
        DefaultTaskInstanceExecutor.this.instanceMap.remove(instanceId);
    }

    private void endWithFailed(long instanceId) {
        DefaultTaskInstanceExecutor.this.instanceService.updateStatus(instanceId, TaskInstanceStatus.FAILED);
        DefaultTaskInstanceExecutor.this.instanceMap.remove(instanceId);
        this.noticeService.taskFail(instanceId);
    }

    private void endWithCancelled(long instanceId) {
        DefaultTaskInstanceExecutor.this.instanceService.updateStatus(instanceId, TaskInstanceStatus.CANCELLED);
        DefaultTaskInstanceExecutor.this.instanceMap.remove(instanceId);
    }

    // private void endWithUnknown(long instanceId) {
    // DefaultTaskInstanceExecutor.this.instanceService.updateStatus(instanceId,
    // TaskInstanceStatus.COMPLETE_WITH_UNKNOWN_STATUS);
    // DefaultTaskInstanceExecutor.this.instanceMap.remove(instanceId);
    // }

    public TaskInstanceStatus getStatus(long instanceId) {
        if (isRunning(instanceId)) {
            return TaskInstanceStatus.RUNNING;
        } else {
            return this.instanceService.get(instanceId).getStatus();
        }
    }

    private static class Pair {

        Task         task;
        TaskInstance instance;
        boolean      timeout;

        public Pair(Task task, TaskInstance instance) {
            super();
            this.task = task;
            this.instance = instance;
        }
    }

    private boolean isRunning(long instanceId) {
        Pair pair = this.instanceMap.get(instanceId);
        return (pair != null) && (pair.instance.getStatus() == TaskInstanceStatus.RUNNING);
    }

    private void notifySuccessIfTimeout(Pair pair) {
        if (pair.timeout) {
            // 如果已经超时，则发成功的通知
            this.noticeService.taskSuccess(pair.instance.getId());
        }
    }

    private boolean checkTimeout(Pair pair) {
        if (pair.timeout) {
            // 如果已经超时，说明已经检查过，不再检查
            return true;
        }
        long timeout = pair.task.getTimeout() * 60 * 1000L;
        Date startTime = pair.instance.getScheduleTime();
        if (startTime == null) {
            startTime = pair.instance.getStartTime();
        }
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime.getTime();
            if (duration > timeout) {
                // 报警
                pair.timeout = true;
                this.noticeService.taskTimeout(pair.instance.getId());
                return true;
            }
        }
        return false;
    }

    public void cancel(long instanceId) {
        // 运行中的task才可以取消
        Pair pair = DefaultTaskInstanceExecutor.this.instanceMap.get(instanceId);
        if (pair != null) {
            if (!pair.instance.getStatus().isCompleted()) {
                this.instancesToBeCancel.add(instanceId);
            }
        }
    }

}
