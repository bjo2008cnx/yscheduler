package com.yeahmobi.yscheduler.agentframework.agent.task.transaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.TaskLog;
import com.yeahmobi.yscheduler.agentframework.exception.TaskNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author Leo.Liang
 */
public class FileBasedTaskTransaction<T extends AgentTask> implements TaskTransaction<T> {

    private static final String CONTEXT_ENCODE = "UTF-8";

    private static final Logger log            = LoggerFactory.getLogger(FileBasedTaskTransaction.class);

    private static final String FILE_NAME_META = "tx.meta";
    private static final String FILE_NAME_LOG  = "tx.log";

    private static final String DATE_FORMAT    = "yyyy-MM-dd HH:mm:ss";
    private static final String META_ENCODING  = "utf-8";

    private long                id;
    private T                   task;
    private File                baseDir;
    private File                contextBaseDir;
    private Meta                meta;

    private OutputStream        logOutputStream;
    private boolean             cancelled;

    private Context             context;

    public FileBasedTaskTransaction(long txId, T task, File baseDir) {
        this.id = txId;
        this.task = task;
        this.baseDir = baseDir;
        if (task != null) {
            try {
                this.contextBaseDir = new File(baseDir.getCanonicalPath() + '/' + task.getEventType());
            } catch (IOException e) {
                throw new RuntimeException("Error creating contextBaseDir", e);
            }
        }
    }

    public void info(String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        try {
            IOUtils.write(String.format("[INFO][%s] %s", sdf.format(new Date()), msg), this.getOutputStream());
            IOUtils.write(IOUtils.LINE_SEPARATOR, this.getOutputStream());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void error(String errorMsg, Throwable t) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        try {
            IOUtils.write(String.format("[ERROR][%s] %s", sdf.format(new Date()), errorMsg), this.getOutputStream());
            IOUtils.write(IOUtils.LINE_SEPARATOR, this.getOutputStream());
            t.printStackTrace(new PrintStream(this.getOutputStream()));
            IOUtils.write(IOUtils.LINE_SEPARATOR, this.getOutputStream());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private OutputStream getOutputStream() throws IOException {
        if (this.logOutputStream == null) {
            synchronized (this) {
                if (this.logOutputStream == null) {
                    this.logOutputStream = FileUtils.openOutputStream(getLogFile(), true);
                }
            }
        }

        return this.logOutputStream;
    }

    private File getLogFile() {
        return new File(this.baseDir, FILE_NAME_LOG);
    }

    private void endWithFail(Integer returnValue) {
        try {
            this.meta.setEnd(new Date());
            this.meta.setStatus(TaskTransactionStatus.FAIL);
            this.meta.setReturnValue(returnValue);
            flushMeta();
            close();
        } catch (Throwable e) {
            log.warn("maybe meta file has been close, can be ignored", e);
        }
    }

    private void endWithSuccess(Integer returnValue) {
        try {
            this.meta.setEnd(new Date());
            this.meta.setStatus(TaskTransactionStatus.SUCCESS);
            this.meta.setReturnValue(returnValue);
            flushMeta();
            close();
        } catch (Throwable e) {
            log.warn("maybe meta file has been close, can be ignored", e);
        }
    }

    private void endWithCancel(Integer returnValue) {
        try {
            this.meta.setStatus(TaskTransactionStatus.CANCEL);
            this.meta.setEnd(new Date());
            this.meta.setReturnValue(returnValue);
            flushMeta();
            close();
        } catch (Throwable e) {
            log.warn("maybe meta file has been close, can be ignored", e);
        }
    }

    public long getId() {
        return this.id;
    }

    public TaskLog getLog(long offset, int length) {
        File logFile = getLogFile();
        if (logFile.exists()) {
            FileInputStream fi = null;
            try {
                fi = new FileInputStream(logFile);
                if ((offset < 0) || (length < 0)) {
                    return new TaskLog(new byte[0], 0);
                } else {
                    if (logFile.length() <= offset) {
                        return new TaskLog(new byte[0], 0);
                    } else {
                        fi.skip(offset);
                        byte[] buffer = new byte[length];
                        int remaining = length;
                        while (remaining > 0) {
                            int location = length - remaining;
                            int count = fi.read(buffer, 0 + location, remaining);
                            if (count == -1) { // EOF
                                break;
                            }
                            remaining -= count;
                        }
                        int readLength = length - remaining;

                        return new TaskLog(buffer, readLength);
                    }
                }

            } catch (Throwable e) {
                log.error(String.format("Exception occurs while getting log(txId=%s)", Long.toString(this.id)), e);
            } finally {
                IOUtils.closeQuietly(fi);
            }
        }

        return new TaskLog(new byte[0], 0);
    }

    @SuppressWarnings("unchecked")
    public void cancel() {
        try {
            this.task.getTaskExecutor().cancel(this);
            this.cancelled = true;
        } catch (Exception e) {
            error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public void execute() {
        // 如果meta状态是结束，则说明已经结束，不再运行
        if (this.meta.getStatus().isCompleted()) {
            return;
        }

        try {
            Integer returnValue = null;

            // 执行任务
            if (this.meta.getStatus() != TaskTransactionStatus.RUNNING) {
                // 第一次运行
                // 设置tx状态为运行中
                this.meta.setStatus(TaskTransactionStatus.RUNNING);
                flushMeta();
                returnValue = this.task.getTaskExecutor().execute(this);
            } else {
                // 恢复
                returnValue = this.task.getTaskExecutor().recover(this);
            }

            if (this.cancelled) {
                endWithCancel(returnValue);
            } else if ((returnValue != null) && (returnValue != 0)) {
                endWithFail(returnValue);
            } else {
                endWithSuccess(returnValue);
            }

        } catch (Throwable e) {
            error(e.getMessage(), e);
            endWithFail(null);
        } finally {
            close();
        }

    }

    public void close() {
        IOUtils.closeQuietly(this.logOutputStream);
    }

    public void init() {
        Map<String, String> params = this.task.getTaskParams();

        this.meta = new Meta(this.task.getEventType(), params, TaskTransactionStatus.INIT, new Date(), null, null);
        flushMeta();

        this.context = new Context();
    }

    public void load() throws TaskNotFoundException {
        try {
            this.meta = loadMeta();
            this.context = loadContext();
        } catch (Exception e) {
            throw new TaskNotFoundException(String.format("Task with transaction id {%s} not found",
                                                          String.valueOf(this.id)), e);
        }
    }

    public Meta getMeta() {
        return this.meta;
    }

    private Meta loadMeta() throws IOException, NumberFormatException, JSONException, ParseException {
        return Meta.valueOf(FileUtils.readFileToString(getMetaFile(), META_ENCODING));
    }

    private Context loadContext() throws ParseException, IOException {
        Context context = new Context();

        if ((this.contextBaseDir != null) && this.contextBaseDir.exists()) {

            Collection<File> files = FileUtils.listFiles(this.contextBaseDir, null, false);

            for (File file : files) {
                String name = file.getName();
                String value = FileUtils.readFileToString(file, CONTEXT_ENCODE);
                context.put(name, value);
            }
        }
        return context;
    }

    private File getMetaFile() {
        return new File(this.baseDir, FILE_NAME_META);
    }

    private void flushMeta() {
        try {
            FileUtils.writeStringToFile(getMetaFile(), this.meta.toString(), META_ENCODING);
        } catch (Throwable e) {
            log.warn("Flush meta file failed", e);
        }
    }

    public T getTask() {
        return this.task;
    }

    public Context getContext() {
        return this.context;
    }

    public void persistContext() throws IOException {
        // 清空this.contextBaseDir
        FileUtils.deleteDirectory(this.contextBaseDir);

        for (Map.Entry<String, Object> entry : this.context.entrySet()) {
            String key = entry.getKey();
            String value = JSON.toJSONString(entry.getValue());
            File file = new File(this.contextBaseDir, key);
            FileUtils.write(file, value, CONTEXT_ENCODE);
        }
    }

}
