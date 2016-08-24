package com.yeahmobi.yscheduler.agentframework.agent.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Leo Liang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskLog {
    private byte[] data;
    private int length;
}
