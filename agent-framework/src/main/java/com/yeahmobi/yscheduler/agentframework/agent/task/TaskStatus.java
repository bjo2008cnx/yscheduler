package com.yeahmobi.yscheduler.agentframework.agent.task;

import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Leo.Liang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatus {
    private TaskTransactionStatus status;
    private long duration;
    private Integer returnValue;
}
