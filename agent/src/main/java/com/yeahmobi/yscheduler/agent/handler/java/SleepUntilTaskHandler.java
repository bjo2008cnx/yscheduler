package com.yeahmobi.yscheduler.agent.handler.java;

import com.yeahmobi.yscheduler.agent.handler.JavaTaskHandler;
import com.yeahmobi.yscheduler.agentframework.agent.task.BaseTaskExecutor;
import com.yeahmobi.yscheduler.agentframework.agent.task.TaskTransaction;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SleepUntilTaskHandler extends BaseTaskExecutor implements JavaTaskHandler {
    private Map<String, String> params;
    private volatile boolean    cancelled = false;

    public SleepUntilTaskHandler(Map<String, String> params) {
        this.params = params;
    }

    public Integer execute(TaskTransaction taskTransaction) {
        taskTransaction.info("SleepUntil task begin...");
        String absTimeStr = this.params.get("absTime");
        if (StringUtils.isNumeric(absTimeStr)) {
            long absTime = Long.valueOf(absTimeStr);
            while (!this.cancelled && (System.currentTimeMillis() < absTime)) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    taskTransaction.info("Interrupted");
                }
            }
        }

        taskTransaction.info("SleepUntil task end...");

        return 0;
    }

    public void cancel(TaskTransaction taskTransaction) throws Exception {
        this.cancelled = true;
    }

}
