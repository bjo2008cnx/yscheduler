package com.yeahmobi.yscheduler.agentframework.agent.task.transaction;

import com.yeahmobi.yscheduler.agentframework.agent.task.agenttask.AgentTask;
import com.yeahmobi.yscheduler.agentframework.exception.TaskNotFoundException;
import com.yeahmobi.yscheduler.agentframework.exception.TaskTransactionCreationException;
import com.yeahmobi.yscheduler.agentframework.exception.TaskTransactionManagerInitializeFailException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Leo.Liang
 */
public class FileBasedTaskTransactionManager implements TaskTransactionManager {

    private static final Logger log = LoggerFactory.getLogger(FileBasedTaskTransactionManager.class);
    private File baseDir;
    private String baseDirStr;
    private TransactionIdGenerator idGenerator;
    private long preservedDay = 2;
    private ScheduledExecutorService cleanTaskExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Agent-CleanerThread");
            return t;
        }
    });

    public void setBaseDir(String baseDirStr) {
        this.baseDirStr = baseDirStr;
        this.baseDir = new File(baseDirStr);
    }

    public String getBaseDir() {
        return this.baseDirStr;
    }

    public void setPreservedDay(long preservedDay) {
        this.preservedDay = preservedDay;
    }

    @SuppressWarnings("unchecked")
    public TaskTransaction getTransaction(long transactionId) throws TaskNotFoundException {
        FileBasedTaskTransaction tx = new FileBasedTaskTransaction(transactionId, null, new File(this.baseDir, String.valueOf(transactionId)));
        tx.load();
        return tx;
    }

    @SuppressWarnings("unchecked")
    public TaskTransaction getTransaction(long transactionId, AgentTask task) throws TaskTransactionCreationException, TaskNotFoundException {
        FileBasedTaskTransaction tx = new FileBasedTaskTransaction(transactionId, task, new File(this.baseDir, String.valueOf(transactionId)));
        tx.load();
        return tx;
    }

    public void init() throws TaskTransactionManagerInitializeFailException {
        try {
            ensureDirectoryExists(this.baseDir);
            this.cleanTaskExecutor.scheduleAtFixedRate(new Runnable() {

                public void run() {
                    File[] files = FileBasedTaskTransactionManager.this.baseDir.listFiles();
                    long lastPreservedTime = new Date().getTime() - (FileBasedTaskTransactionManager.this.preservedDay * 24 * 60 * 60 * 1000L);
                    for (File file : files) {
                        if (file.isDirectory() && StringUtils.isNumeric(file.getName())) {
                            if (file.lastModified() < lastPreservedTime) {
                                FileUtils.deleteQuietly(file);
                            }
                        }
                    }

                }
            }, 1, 60, TimeUnit.MINUTES);
            this.idGenerator = new TransactionIdGenerator(this.baseDir);
            this.idGenerator.init();
        } catch (Throwable e) {
            log.error("FileBasedTaskTransactionManager init failed.", e);
            throw new TaskTransactionManagerInitializeFailException("FileBasedTaskTransactionManager init failed.", e);
        }
    }

    private void ensureDirectoryExists(File directory) throws IOException {
        FileUtils.forceMkdir(directory);
    }

    @SuppressWarnings("unchecked")
    public TaskTransaction createTransaction(AgentTask task) throws TaskTransactionCreationException {
        long txId = this.idGenerator.nextId();
        FileBasedTaskTransaction tx = new FileBasedTaskTransaction(txId, task, new File(this.baseDir, String.valueOf(txId)));
        tx.init();
        return tx;
    }

    public List<TaskTransaction> getAllTransaction() throws TaskNotFoundException {
        List<TaskTransaction> list = new ArrayList<TaskTransaction>();
        File[] files = FileBasedTaskTransactionManager.this.baseDir.listFiles();
        for (File file : files) {
            try {
                if (file.isDirectory() && StringUtils.isNumeric(file.getName())) {
                    long transactionId = Long.parseLong(file.getName());
                    TaskTransaction tx = this.getTransaction(transactionId);
                    list.add(tx);
                }
            } catch (Exception e) {
                log.error(String.format("Error when load transaction(txId=%s), skip this transaction.", file.getName()), e);
            }
        }
        return list;
    }

    private static class TransactionIdGenerator {

        private static final String ID_STORAGE_FILE_NAME = "txid";
        private static final int FILE_SIZE = 20;
        private File baseDir;
        private File idStorageFile;
        private MappedByteBuffer mbb;
        private static final byte[] BUF_MASK = new byte[FILE_SIZE];
        private AtomicLong currentId = new AtomicLong(0);

        public TransactionIdGenerator(File baseDir) {
            this.baseDir = baseDir;
            this.idStorageFile = new File(this.baseDir, ID_STORAGE_FILE_NAME);
        }

        public void init() throws IOException {
            ensureStorageExists();
            initStorage();
        }

        @SuppressWarnings("resource")
        private void initStorage() throws IOException {
            this.mbb = new RandomAccessFile(this.idStorageFile, "rw").getChannel().map(MapMode.READ_WRITE, 0, FILE_SIZE);
            loadFromFile();
        }

        private void saveToFile() {
            this.mbb.position(0);
            this.mbb.put(BUF_MASK);
            this.mbb.position(0);
            this.mbb.put(String.valueOf(this.currentId.longValue()).getBytes());
            this.mbb.put(System.getProperty("line.separator").getBytes());
        }

        private void loadFromFile() throws IOException {
            List<String> lines = FileUtils.readLines(this.idStorageFile);

            if ((lines == null) || lines.isEmpty() || (lines.get(0) == null) || !StringUtils.isNumeric(lines.get(0))) {
                // save 0 to file
                saveToFile();
            } else {
                this.currentId.set(Long.valueOf(lines.get(0)));
            }
        }

        private void ensureStorageExists() throws IOException {
            if (this.idStorageFile.exists()) {
                if (this.idStorageFile.isDirectory()) {
                    throw new IOException(String.format("File(%s) already exists, but not a file.", this.idStorageFile.getAbsolutePath()));
                }
            } else {
                if (!this.idStorageFile.createNewFile()) {
                    throw new IOException(String.format("Fail to create file(%s).", this.idStorageFile.getAbsolutePath()));
                }
            }
        }

        public long nextId() {
            long nextId = this.currentId.incrementAndGet();
            saveToFile();
            return nextId;
        }

    }

}
