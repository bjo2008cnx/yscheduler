package com.yeahmobi.yscheduler.agentframework.client;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.deserializer.ExtraProcessor;
import com.alibaba.fastjson.parser.deserializer.ExtraTypeProvider;
import com.yeahmobi.yscheduler.agentframework.AgentRequest;
import com.yeahmobi.yscheduler.agentframework.AgentResponse;
import com.yeahmobi.yscheduler.agentframework.AgentResponseCode;
import com.yeahmobi.yscheduler.agentframework.agent.task.TaskLog;
import com.yeahmobi.yscheduler.agentframework.agent.task.TaskStatus;
import com.yeahmobi.yscheduler.agentframework.agent.task.transaction.TaskTransactionStatus;
import com.yeahmobi.yscheduler.agentframework.exception.AgentClientException;

/**
 * @author Leo.Liang
 */
public class DefaultAgentClient implements AgentClient {

    private static final Logger log             = LoggerFactory.getLogger(DefaultAgentClient.class);

    private static final int    DEFAULT_PORT    = 24368;
    private static final String EVENT_TYPE_PING = "ping";
    private int                 port            = DEFAULT_PORT;
    private String              agentName;
    private int                 connectTimeout  = 1000;
    private int                 socketTimeout   = 3000;

    /**
     * @param connectTimeout the connectTimeout to set
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * @param socketTimeout the socketTimeout to set
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public DefaultAgentClient(String agentName) {
        this(DEFAULT_PORT, agentName);
    }

    public DefaultAgentClient(int port, String agentName) {
        this.port = port;
        this.agentName = agentName;
    }

    /**
     * @param agentName the agentName to set
     */
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    private URI buildURI(String host, String eventType) throws URISyntaxException {
        return new URI(String.format("http://%s:%d/%s/?" + AgentRequest.REQKEY_EVENT_TYPE + "=" + eventType, host,
                                     this.port, this.agentName));
    }

    public <T> AgentResponse<T> call(String host, AgentRequest request) throws AgentClientException {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put(AgentRequest.REQKEY_PARAMS, JSON.toJSONString(request.getParams()));
            return post(buildURI(host, request.getEventType()), params);
        } catch (Exception e) {
            log.error("Fail to call agent.", e);
            throw new AgentClientException("Fail to call agent.", e);
        }
    }

    public boolean ping(String host) {
        try {
            AgentResponse<?> resp = call(host, new AgentRequest(EVENT_TYPE_PING, null));

            if ((resp != null) && AgentResponseCode.SUCCESS.equals(resp.getResponseCode())) {
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }

    }

    @SuppressWarnings("unchecked")
    private <T> AgentResponse<T> post(URI uri, Map<String, String> params) throws ClientProtocolException, IOException,
                                                                          AgentClientException {
        RequestBuilder requestBuilder = RequestBuilder.post().setUri(uri).setConfig(buildConfig());

        for (Map.Entry<String, String> entry : params.entrySet()) {
            requestBuilder.addParameter(entry.getKey(), entry.getValue());
        }

        HttpUriRequest uriRequest = requestBuilder.build();

        CloseableHttpClient httpclient = HttpClientBuilder.create().build();

        CloseableHttpResponse response = httpclient.execute(uriRequest);
        try {
            StatusLine statusLine = response.getStatusLine();
            if (HttpStatus.SC_OK == statusLine.getStatusCode()) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return JSON.parseObject(IOUtils.toString(entity.getContent()), AgentResponse.class,
                                            new AgentResponseDataProcessor());
                } else {
                    throw new AgentClientException("No response data");
                }
            } else {
                throw new AgentClientException(String.format("Agent unavailable. (responseCode=%d, reason=%s)",
                                                             statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }

        } finally {
            httpclient.close();
        }
    }

    private static class AgentResponseDataProcessor implements ExtraProcessor, ExtraTypeProvider {

        @SuppressWarnings("unchecked")
        public void processExtra(Object object, String key, Object value) {
            if ((object instanceof AgentResponse) && AgentResponse.FIELD_RESPONSE_DATA.equals(key)) {
                AgentResponse resp = (AgentResponse) object;
                resp.setResponseData(value);
            }
        }

        public Type getExtraType(Object object, String key) {
            if ((object instanceof AgentResponse) && AgentResponse.FIELD_RESPONSE_DATA.equals(key)) {
                AgentResponse resp = (AgentResponse) object;
                try {
                    return Class.forName(resp.getResponseType());
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
            return null;
        }

    }

    private RequestConfig buildConfig() {
        return RequestConfig.custom().setConnectTimeout(this.connectTimeout).setSocketTimeout(this.socketTimeout).build();
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 24368;

        DefaultAgentClient agentClient = new DefaultAgentClient(port, "yscheduler");

        Map<String, String> submitJavaTaskParams = new HashMap<String, String>();
        submitJavaTaskParams.put("times", "1000");
        // submit task
        AgentResponse<Long> res1 = agentClient.call(host, new AgentRequest("javaSample", submitJavaTaskParams));

        if (AgentResponseCode.SUCCESS.equals(res1.getResponseCode())) {
            if (res1.getResponseData() == null) {
                System.out.println("Unexpected response data null while submitting task.");
                return;
            }

            Long txId = res1.getResponseData();
            System.out.println(String.format("Submit task success(txId=%d).", txId));

            Map<String, String> checkStatusParams = new HashMap<String, String>();
            checkStatusParams.put("txId", String.valueOf(txId));
            AgentResponse<TaskStatus> res2 = agentClient.call(host, new AgentRequest("TASK_STATUS", checkStatusParams));

            long offset = 0;
            int length = 1024 * 1024;

            File logFile = new File("E:\\log.txt");

            if (logFile.exists()) {
                FileUtils.forceDelete(logFile);
            }

            while (AgentResponseCode.SUCCESS.equals(res2.getResponseCode())) {
                if (res2.getResponseData() == null) {
                    System.out.println("Unexpected response data null while checking task status.");
                    break;
                } else {
                    TaskStatus taskStatus = res2.getResponseData();
                    TaskTransactionStatus transactionStatus = taskStatus.getStatus();

                    if (transactionStatus == null) {
                        System.out.println("Unexpected response data null while checking transaction status.");
                        break;
                    } else {
                        if ((transactionStatus != null) && !TaskTransactionStatus.RUNNING.equals(transactionStatus)
                            && !TaskTransactionStatus.INIT.equals(transactionStatus)) {

                            System.out.println(String.format("Task complete with status %s.", transactionStatus));

                            // get the remaining log
                            while (true) {
                                Map<String, String> getLogParams = new HashMap<String, String>();
                                getLogParams.put("txId", String.valueOf(txId));
                                getLogParams.put("offset", String.valueOf(offset));
                                getLogParams.put("length", String.valueOf(length));

                                AgentResponse<TaskLog> res3 = agentClient.call(host, new AgentRequest("TASK_LOG",
                                                                                                      getLogParams));

                                if (res3.getResponseData() == null) {
                                    System.out.println("Unexpected response data null while getting log.");
                                    break;
                                } else {
                                    TaskLog log = res3.getResponseData();

                                    if (log == null) {
                                        System.out.println("Unexpected log data null while getting log.");
                                    } else {

                                        if (log.getLength() > 0) {
                                            System.out.println(String.format("Log received %s bytes", log.getLength()));
                                            byte[] data = new byte[log.getLength()];
                                            System.arraycopy(log.getData(), 0, data, 0, log.getLength());
                                            FileUtils.writeByteArrayToFile(logFile, data, true);
                                            offset += log.getLength();
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }

                            // System.out.println("Log file:");
                            // System.out.println(FileUtils.readFileToString(logFile));
                            break;
                        } else {
                            Map<String, String> getLogParams = new HashMap<String, String>();
                            getLogParams.put("txId", String.valueOf(txId));
                            getLogParams.put("offset", String.valueOf(offset));
                            getLogParams.put("length", String.valueOf(length));

                            AgentResponse<TaskLog> res3 = agentClient.call(host, new AgentRequest("TASK_LOG",
                                                                                                  getLogParams));

                            if (res3.getResponseData() == null) {
                                System.out.println("Unexpected response data null while getting log.");
                                break;
                            } else {
                                TaskLog log = res3.getResponseData();

                                if (log == null) {
                                    System.out.println("Unexpected log data null while getting log.");
                                } else {
                                    System.out.println(String.format("Log received %s bytes", log.getLength()));
                                    byte[] data = new byte[log.getLength()];
                                    System.arraycopy(log.getData(), 0, data, 0, log.getLength());
                                    FileUtils.writeByteArrayToFile(logFile, data, true);
                                    offset += log.getLength();
                                }
                            }

                            Thread.sleep(1000);
                            System.out.println(String.format("Task running(durationTime=%ds)...",
                                                             taskStatus.getDuration()));
                            res2 = agentClient.call(host, new AgentRequest("TASK_STATUS", checkStatusParams));
                        }
                    }
                }
            }

            if (!AgentResponseCode.SUCCESS.equals(res2.getResponseCode())) {
                System.out.println(String.format("Task status checking failed(errorCode=%s, errorMsg=%s).",
                                                 res2.getResponseCode(), res2.getErrorMsg()));
            }
        } else {
            // submit fail
            System.out.println(String.format("Submit task failed(errorCode=%s, errorMsg=%s).", res1.getResponseCode(),
                                             res1.getErrorMsg()));
        }
        // System.out.println(agentClient.ping(host));

    }
}
