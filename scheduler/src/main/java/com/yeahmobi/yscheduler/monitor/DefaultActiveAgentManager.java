package com.yeahmobi.yscheduler.monitor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yeahmobi.yscheduler.model.Agent;
import com.yeahmobi.yscheduler.model.service.AgentService;
import com.yeahmobi.yscheduler.monitor.ActiveAgentManager;

@Service
public class DefaultActiveAgentManager implements ActiveAgentManager {

    /** 30min 清理一下过期数据 */
    private static final int              CLEAR_INTERVAL            = 30 * 60 * 1000;

    // <agentId,lastHeartbeatTime>
    private ConcurrentHashMap<Long, Long> agentId2LastHeartbeatTime = new ConcurrentHashMap<Long, Long>();

    // <agentId,version+putTime>
    private ConcurrentHashMap<Long, Pair> agentId2Version           = new ConcurrentHashMap<Long, Pair>();

    @Autowired
    private AgentService                  agentService;

    // default 3min
    private long                          thresholdMillis           = 180000;

    private long                          lastClearTime             = System.currentTimeMillis();

    // 从数据库加载所有agent(enable)
    // 加载后，默认都是active的agent
    // 本来是不需要加载，只要heartbeat时记录即可。但是，就怕在启动后，在heartbeat到来之前，正常的task就获取不到可用的agent
    @PostConstruct
    public void init() {
        List<Agent> list = this.agentService.list();
        long curTime = System.currentTimeMillis();
        for (Agent agent : list) {
            if (agent.getEnable()) {
                this.agentId2LastHeartbeatTime.put(agent.getId(), curTime);
            }
        }
    }

    // 新增agent后，agent heartbeat会出现，进而会调用该方法，此时能自动追加active列表（所以在agent时，无须处理）
    public void heartbeat(long agentId) {
        long curTime = System.currentTimeMillis();
        this.agentId2LastHeartbeatTime.put(agentId, curTime);

        // 每30分钟，清理一次数据(把过期的清除)
        if ((curTime - this.lastClearTime) > CLEAR_INTERVAL) {
            for (Long agentId0 : this.agentId2LastHeartbeatTime.keySet()) {
                Long lastHeartbeatTime = this.agentId2LastHeartbeatTime.get(agentId0);
                if (lastHeartbeatTime != null) {
                    long duration = System.currentTimeMillis() - lastHeartbeatTime;
                    if (duration > this.thresholdMillis) {
                        this.agentId2LastHeartbeatTime.remove(agentId0);
                    }
                }
            }
            this.lastClearTime = curTime;
        }
    }

    public void checkAndUpdateAgentVersion(long agentId, String agentVersion) {
        if (StringUtils.isNotBlank(agentVersion)) {
            if (this.agentId2Version.containsKey(agentId)) {
                if (!this.agentId2Version.get(agentId).getVersion().equals(agentVersion)) {
                    this.agentService.updateVersion(agentId, agentVersion);
                    this.agentId2Version.put(agentId, this.agentId2Version.get(agentId).updateVersion(agentVersion));
                }
            } else {
                String version = this.agentService.get(agentId).getVersion();
                if (!agentVersion.equals(version)) {
                    this.agentService.updateVersion(agentId, agentVersion);
                }
                this.agentId2Version.put(agentId, new Pair(agentVersion));
            }
        }
        // 每30分钟，清理一次数据(把过期的清除)
        clearAgentVersionCache();
    }

    private void clearAgentVersionCache() {
        long curTime = System.currentTimeMillis();
        if ((curTime - this.lastClearTime) > CLEAR_INTERVAL) {
            for (Long agentId0 : this.agentId2Version.keySet()) {
                Pair pair = this.agentId2Version.get(agentId0);
                long duration = System.currentTimeMillis() - pair.putTime;
                if (duration > CLEAR_INTERVAL) {
                    this.agentId2Version.remove(agentId0);
                }
            }
            this.lastClearTime = curTime;
        }
    }

    public boolean isActive(long agentId) {
        Long lastHeartbeatTime = this.agentId2LastHeartbeatTime.get(agentId);
        if (lastHeartbeatTime == null) {
            lastHeartbeatTime = -1L;
        }
        long duration = System.currentTimeMillis() - lastHeartbeatTime;
        if (duration > this.thresholdMillis) {
            this.agentId2LastHeartbeatTime.remove(agentId);
            return false;
        } else {
            return true;
        }
    }

    public void setThresholdMinute(long thresholdMinute) {
        this.thresholdMillis = thresholdMinute * 60 * 1000;
    }

    public List<Agent> getActiveList(long teamId) {
        List<Agent> list = this.agentService.list(teamId, true);

        Iterator<Agent> iterator = list.iterator();
        while (iterator.hasNext()) {
            Agent agent = iterator.next();
            Long agentId = agent.getId();
            if (!isActive(agentId)) {
                iterator.remove();
            }
        }

        return list;

    }

    private static class Pair {

        String version;
        long   putTime;

        public Pair(String version) {
            super();
            this.version = version;
            this.putTime = System.currentTimeMillis();
        }

        public Pair updateVersion(String version) {
            this.version = version;
            this.putTime = System.currentTimeMillis();
            return this;
        }

        public String getVersion() {
            return this.version;
        }

    }
}
