package com.yeahmobi.yscheduler.agentframework.agent.task.executor;

import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.ShellAgentTask;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.FileBasedTaskTransactionManager;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransaction;
import com.yeahmobi.yscheduler.agentframework.common.TaskLocks;
import com.yeahmobi.yscheduler.agentframework.common.HttpClientUtil;
import com.yeahmobi.yscheduler.common.variable.VariableException;
import com.yeahmobi.yscheduler.common.variable.VariableManager;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Leo.Liang
 */
public class ShellTaskExecutor extends BaseTaskExecutor<ShellAgentTask> {

    // private static final Logger LOGGER = LoggerFactory.getLogger(ShellTaskExecutor.class);

    private static final String SHELL_SUFFIX = ".sh";

    private static final String FILESERVER_NAMESPACE = "task";

    private static final long CHECK_INTERVAL = 1;

    private String baseDir;
    private String shellDir;
    private String taskContextBaseDir;

    private FileBasedTaskTransactionManager taskTransactionManager;

    private VariableManager variableManager;

    public void setVariableManager(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    public void setTaskTransactionManager(FileBasedTaskTransactionManager transactionManager) {
        this.taskTransactionManager = transactionManager;
    }

    /**
     * 初始化
     *
     * @throws IOException
     */
    public void init() throws IOException {
        // 复制shell到 baseDir/shell
        this.baseDir = this.taskTransactionManager.getBaseDir();
        this.shellDir = this.baseDir + "/shell";

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(getShellPathPattern());

        File shellDirFile = new File(this.shellDir);
        FileUtils.forceMkdir(shellDirFile);

        for (Resource resource : resources) {
            InputStream stream = resource.getInputStream();
            try {
                FileUtils.copyInputStreamToFile(stream, new File(shellDirFile, resource.getFilename()));
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        File taskContextBaseDirFile = new File(this.taskContextBaseDir);
        FileUtils.forceMkdir(taskContextBaseDirFile);
    }

    private String getShellPathPattern() {
        return "classpath*:/" + ShellTaskExecutor.class.getPackage().getName().replace('.', '/') + "/*.sh";
    }

    /**
     * 执行任务事务
     *
     * @param taskTransaction
     * @return
     * @throws ExecuteException
     * @throws IOException
     * @throws InterruptedException
     * @throws VariableException
     */
    public Integer execute(TaskTransaction<ShellAgentTask> taskTransaction) throws ExecuteException, IOException, InterruptedException, VariableException {
        Integer exitCode = null;

        long txId = taskTransaction.getId();

        taskTransaction.info("Shell task started ( transaction is " + txId + " )");

        ShellAgentTask task = taskTransaction.getTask();

        boolean everStarted = everStarted(txId, task.getEventType());
        // 从未执行过，则run
        if (!everStarted) {
            // 内置变量的替换
            task.setCommand(this.variableManager.process(task.getCommand(), null));
            // 下载附件
            if (hasAttachment(task)) {
                taskTransaction.info("Attachment version: " + task.getAttachmentVersion());
                downloadAttachment(task, taskTransaction);
            }
            // 执行shell
            runShell(taskTransaction, txId, task);
        }

        // 轮询pid和exitCode
        while (checkRunningByPid(txId, task.getEventType())) {
            TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
        }

        exitCode = getExitCode(txId, task.getEventType());

        return exitCode;

    }

    /**
     * 下载附件
     *
     * @param task
     * @param taskTransaction
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void downloadAttachment(ShellAgentTask task, TaskTransaction<ShellAgentTask> taskTransaction) throws FileNotFoundException, IOException {
        Long attachmentVersion = task.getAttachmentVersion();
        // 确认目录是否存在，不存在，或存在版本小，则下载
        boolean needDownload = true;
        File contextParentDir = new File(getContextParentDir(task));
        File contextDir = new File(getContextDir(task));
        File versionFile = null;
        if (contextParentDir.exists()) {
            versionFile = new File(contextParentDir, ".version");
            if (versionFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(versionFile);
                try {
                    String versionString = IOUtils.toString(fileInputStream);
                    if (StringUtils.isNotBlank(versionString)) {
                        Long existsAttachmentVersion = Long.parseLong(versionString);
                        taskTransaction.info("Exists Attachment version: " + existsAttachmentVersion);
                        if (existsAttachmentVersion >= attachmentVersion) {
                            needDownload = false;
                        }
                    }
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                }
            }
        }
        if (needDownload) {
            // 相同task，要串行下载，才能保护数据正确
            ReentrantLock lock = TaskLocks.getLock(task.getTaskName());
            try {
                lock.lock();

                File tempAttachmentDir = new File(getTempAttachmentDir(task));
                FileUtils.forceMkdir(tempAttachmentDir);
                // 直接下载
                String uri = task.getAttachmentServerUri();
                Map<String, String> params = new HashMap<String, String>();
                params.put("nameSpace", FILESERVER_NAMESPACE);
                params.put("key", task.getTaskName());
                params.put("version", String.valueOf(attachmentVersion));

                taskTransaction.info(String.format("Start to download attachment from %s", uri));
                CloseableHttpResponse response = HttpClientUtil.getResponse(uri, params);
                try {
                    Header filenameHeader = response.getFirstHeader("filename");
                    String filename = filenameHeader != null ? filenameHeader.getValue() : "unknown";

                    taskTransaction.info(String.format("Attachment filename is %s", filename));

                    // 通过filename判断是zip还是shell
                    boolean isZip = false;
                    if (StringUtils.endsWithIgnoreCase(filename, ".zip")) {
                        isZip = true;
                    }

                    // 下载附件（并解压到指定目录）
                    InputStream content = response.getEntity().getContent();
                    if (isZip) {
                        unzip(tempAttachmentDir, content, taskTransaction);
                    } else {
                        File dstFile = new File(tempAttachmentDir, filename);
                        FileOutputStream dstFileOutput = new FileOutputStream(dstFile);
                        try {
                            IOUtils.copyLarge(content, dstFileOutput);
                        } finally {
                            IOUtils.closeQuietly(dstFileOutput);
                        }
                        makeExecutable(dstFile);
                    }
                    taskTransaction.info("Attachment downloaded");

                    // mv tempAttachment to Attachment
                    FileUtils.deleteDirectory(contextDir);
                    FileUtils.moveDirectory(tempAttachmentDir, contextDir);

                    // 更新.version文件
                    OutputStream output = new FileOutputStream(new File(contextParentDir, ".version"));
                    try {
                        IOUtils.write(String.valueOf(attachmentVersion), output);
                    } finally {
                        IOUtils.closeQuietly(output);
                    }
                    taskTransaction.info(String.format("Attachment version updated to %s", attachmentVersion));

                } finally {
                    response.close();
                    FileUtils.deleteDirectory(tempAttachmentDir);// ensure delete temp
                }
            } finally {
                lock.unlock();
            }

        } else {
            taskTransaction.info("Need not download attachment.");
            // 有带version带不需要下载，也touch一下version，表示附件是有被访问的（因为后续会定期会清理长时间未访问的附件）
            versionFile.setLastModified(System.currentTimeMillis());
        }
    }

    private void makeExecutable(File dstFile) {
        // 如果是shell，则设置成可执行
        if (StringUtils.endsWithIgnoreCase(dstFile.getName(), SHELL_SUFFIX)) {
            dstFile.setExecutable(true, false);
        }
    }

    private boolean hasAttachment(ShellAgentTask task) {
        Long attachmentVersion = task.getAttachmentVersion();
        return attachmentVersion != null;
    }

    private String getContextParentDir(ShellAgentTask task) {
        return this.taskContextBaseDir + "/" + task.getTaskName();
    }

    private String getContextDir(ShellAgentTask task) {
        return this.taskContextBaseDir + "/" + task.getTaskName() + "/context";
    }

    private String getTempAttachmentDir(ShellAgentTask task) {
        return this.taskContextBaseDir + "/" + task.getTaskName() + "/temp-" + System.currentTimeMillis();
    }

    /**
     * 解压缩
     *
     * @param attachmentDir
     * @param content
     * @param taskTransaction
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void unzip(File attachmentDir, InputStream content, TaskTransaction<ShellAgentTask> taskTransaction) throws IOException, FileNotFoundException {
        ZipArchiveInputStream zipInput = new ZipArchiveInputStream(content);
        try {
            ArchiveEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                File dstFile = new File(attachmentDir, entry.getName());
                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(dstFile);
                } else {
                    taskTransaction.info(String.format("Downloading(unzip) %s (size:%sbytes)", entry.getName(), entry.getSize()));
                    byte[] bytes = new byte[(int) entry.getSize()];
                    int readSizes = 0;
                    while (readSizes < bytes.length) {
                        int n = zipInput.read(bytes, readSizes, bytes.length - readSizes);
                        if (n == -1) {
                            break;
                        } else {
                            readSizes += n;
                        }
                    }
                    OutputStream output = new FileOutputStream(dstFile);
                    try {
                        IOUtils.write(bytes, output);
                    } finally {
                        IOUtils.closeQuietly(output);
                    }
                    taskTransaction.info(String.format("Downloaded %s", entry.getName(), entry.getSize()));
                    makeExecutable(dstFile);
                }
            }
        } finally {
            IOUtils.closeQuietly(zipInput);
        }
    }

    /**
     * 取消任务,杀死进程的方式比较粗暴 TODO 优化
     *
     * @param taskTransaction
     * @throws ExecuteException
     * @throws IOException
     */
    public void cancel(TaskTransaction taskTransaction) throws ExecuteException, IOException {
        long txId = taskTransaction.getId();
        String eventType = taskTransaction.getMeta().getEventType();

        CommandLine cmdLine = new CommandLine("bash");
        cmdLine.addArgument(this.baseDir + "/shell/cancel.sh");
        cmdLine.addArgument(this.baseDir);
        cmdLine.addArgument(String.valueOf(txId));
        cmdLine.addArgument(eventType);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(new int[]{0});

        executor.execute(cmdLine);
    }

    /**
     * 运行shell
     *
     * @param taskTransaction
     * @param txId
     * @param task
     * @throws ExecuteException
     * @throws IOException
     */
    private void runShell(final TaskTransaction taskTransaction, long txId, ShellAgentTask task) throws ExecuteException, IOException {
        CommandLine cmdLine = new CommandLine("bash");
        cmdLine.addArgument(this.baseDir + "/shell/run.sh");
        cmdLine.addArgument(this.baseDir);
        cmdLine.addArgument(String.valueOf(txId));
        cmdLine.addArgument(task.getEventType());
        cmdLine.addArgument(task.getCommand(), false);

        // 运行上下文目录(shell会在自己的上下文目录运行，这样shell若有下载文件等操作，就不会互相影响)
        // 注意：原来打算 command = "cd " + getAttachmentDir(task) + ";" + command;
        // 后来，“进去附件的操作”改在run.sh中，因为bash -c "多指令"时pid不准确，cancel有影响；
        String contextDir = getContextDir(task);
        FileUtils.forceMkdir(new File(contextDir));
        cmdLine.addArgument(contextDir);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(null);

        executor.execute(cmdLine);
    }

    private boolean everStarted(long txId, String eventType) {
        // 是否有pid
        File file = new File(this.baseDir + "/" + txId + "/" + eventType + "/pid");
        return file.exists();
    }

    private Integer getExitCode(long txId, String eventType) {
        File file = new File(this.baseDir + "/" + txId + "/" + eventType + "/exitcode");
        if (file.exists()) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                String exitCodeStr = StringUtils.trim(IOUtils.toString(fileInputStream));
                return Integer.parseInt(exitCodeStr);
            } catch (Exception e) {
                return null;
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        }
        return null;
    }

    private boolean checkRunningByPid(long txId, String eventType) throws ExecuteException, IOException {
        CommandLine cmdLine = new CommandLine("bash");
        cmdLine.addArgument(this.baseDir + "/shell/check.sh");
        cmdLine.addArgument(this.baseDir);
        cmdLine.addArgument(String.valueOf(txId));
        cmdLine.addArgument(eventType);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(null);

        int exitCode = executor.execute(cmdLine);

        return exitCode == 1;
    }

    public void setTaskContextBaseDir(String taskContextBaseDir) {
        this.taskContextBaseDir = taskContextBaseDir;
    }

}
