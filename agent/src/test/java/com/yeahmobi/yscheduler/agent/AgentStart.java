package com.yeahmobi.yscheduler.agent;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Ryan Sun
 */

public class AgentStart {

    /**
     * Jetty server
     */
    private Server server;

    /**
     * 启动Jetty server
     *
     * @throws Exception
     */
    public void startServer() throws Exception {
        this.server = new Server(24368);
        this.server.setStopAtShutdown(true);
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/yscheduler");
        webAppContext.setResourceBase("src/main/webapp");

        webAppContext.setClassLoader(getClass().getClassLoader());
        this.server.setHandler(webAppContext);
        this.server.start();

        waitForAnyKey();

    }

    /**
     * 按任意键停止server
     *
     * @throws IOException
     */
    protected void waitForAnyKey() throws IOException {
        String timestamp = new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(new Date());

        System.out.println(String.format("[%s] [INFO] Press any key to stop server ... ", timestamp));
        System.in.read();
    }

    public void shutdownServer() throws Exception {
        this.server.stop();
    }

    public static void main(String[] args) throws Exception {
        AgentStart server = new AgentStart();
        server.startServer();
        server.shutdownServer();
    }

}
