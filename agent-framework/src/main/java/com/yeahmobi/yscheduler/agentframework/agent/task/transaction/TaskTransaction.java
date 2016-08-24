package com.yeahmobi.yscheduler.agentframework.agent.task.transaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.TaskLog;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Leo.Liang
 */
public interface TaskTransaction<T extends AgentTask> {

    public long getId();

    public void execute();

    public void cancel();

    public Context getContext();

    public void persistContext() throws IOException;

    public TaskLog getLog(long offset, int length);

    public void info(String msg);

    public void error(String errorMsg, Throwable t);

    public Meta getMeta();

    public T getTask();

    class Meta {

        private static final String   LINE_SEPARATOR = System.getProperty("line.separator");
        private static final String   DATE_FORMAT    = "yyyy-MM-dd HH:mm:ss";

        private String                eventType;
        private Map<String, String>   taskParams;
        private TaskTransactionStatus status;
        private Date                  start;
        private Date                  end;
        private Integer               returnValue;

        public Meta(String eventType, Map<String, String> params, TaskTransactionStatus status, Date start, Date end,
                    Integer returnValue) {
            super();
            this.eventType = eventType;
            this.taskParams = params;
            this.status = status;
            this.start = start;
            this.end = end;
            this.returnValue = returnValue;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public TaskTransactionStatus getStatus() {
            return this.status;
        }

        public void setStatus(TaskTransactionStatus status) {
            this.status = status;
        }

        public Date getStart() {
            return this.start;
        }

        public Date getEnd() {
            return this.end;
        }

        public void setEnd(Date end) {
            this.end = end;
        }

        public Integer getReturnValue() {
            return this.returnValue;
        }

        public void setReturnValue(Integer returnValue) {
            this.returnValue = returnValue;
        }

        public long getDuration() {
            if (start == null) {
                return 0L;
            }
            if (end != null) {
                return end.getTime() - start.getTime();
            } else {
                return new Date().getTime() - start.getTime();
            }
        }

        public Map<String, String> getTaskParams() {
            return taskParams;
        }

        public void setTaskParams(Map<String, String> taskParams) {
            this.taskParams = taskParams;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            String paramsStr = JSON.toJSONString(taskParams);

            return this.eventType + LINE_SEPARATOR + paramsStr + LINE_SEPARATOR + this.status + LINE_SEPARATOR
                   + (this.start == null ? "" : sdf.format(this.start)) + LINE_SEPARATOR
                   + (this.end == null ? "" : sdf.format(this.end)) + LINE_SEPARATOR
                   + (this.returnValue == null ? "" : this.returnValue) + LINE_SEPARATOR;
        }

        @SuppressWarnings("unchecked")
        public static Meta valueOf(String src) throws JSONException, NumberFormatException, ParseException {
            String[] arr = StringUtils.splitByWholeSeparatorPreserveAllTokens(src, LINE_SEPARATOR);
            if ((arr != null) && (arr.length <= 5)) {
                // 兼容旧的meta文件
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                TaskTransactionStatus status = TaskTransactionStatus.valueOf(arr[0]);
                if (!status.isCompleted()) {
                    // 对于老的tx，如果未结束，由于没有eventType和command，无法重跑，所以状态设置成unknown
                    status = TaskTransactionStatus.COMPLETE_WITH_UNKNOWN_STATUS;
                }
                return new Meta(null, null, status, StringUtils.isBlank(arr[1]) ? null : sdf.parse(arr[1]),
                                StringUtils.isBlank(arr[2]) ? null : sdf.parse(arr[2]),
                                StringUtils.isBlank(arr[3]) ? null : Integer.valueOf(arr[3]));
            } else if ((arr != null) && (arr.length >= 6)) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                Map<String, String> params0 = JSON.parseObject(arr[1], Map.class);
                return new Meta(arr[0], params0, TaskTransactionStatus.valueOf(arr[2]),
                                StringUtils.isBlank(arr[3]) ? null : sdf.parse(arr[3]),
                                StringUtils.isBlank(arr[4]) ? null : sdf.parse(arr[4]),
                                StringUtils.isBlank(arr[5]) ? null : Integer.valueOf(arr[5]));
            } else {
                return null;
            }
        }
    }

    class Context extends HashMap<String, Object> {

        @Override
        public Object put(String key, Object value) {
            if (!isFilenameValid(key)) {
                throw new IllegalArgumentException(String.format("Key '%s' is invalid", key));
            }
            return super.put(key, value);
        }

        public static boolean isFilenameValid(String file) {
            File f = new File(file);
            try {
                f.getCanonicalPath();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

}
