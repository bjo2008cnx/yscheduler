package com.yeahmobi.yscheduler.web.controller.task;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.yeahmobi.yscheduler.model.Task;
import com.yeahmobi.yscheduler.model.service.TaskService;
import com.yeahmobi.yscheduler.web.controller.AbstractController;

/**
 * @author Ryan Sun
 */
@Controller
@RequestMapping(value = { TaskUpdateController.SCREEN_NAME })
public class TaskUpdateController extends AbstractController {

    public static final String SCREEN_NAME = "task/update";

    @Autowired
    private TaskService        taskService;

    @Autowired
    private TaskHelper         taskHelper;

    private String             uploadPath;

    @Value("#{confProperties['storageServerUri']}")
    private String             storageServerUri;

    @PostConstruct
    public void init() {
        this.uploadPath = this.storageServerUri + "/upload";
    }

    @RequestMapping(value = { "" }, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public Object update(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, Object> map = new HashMap<String, Object>();

        try {
            Task task = this.taskHelper.extractTaskFromRequest(request, false);

            this.taskService.update(task);
            Long taskId = Long.parseLong(request.getParameter("id"));

            String executeType = request.getParameter("executeType");
            if ("groups".equals(executeType)) {
                this.taskService.updateAgentId(taskId, null);
            }
            map.put("uploadPath", this.uploadPath);
            map.put("proxyPath", request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                                 + "/static/cross_domain_proxy.html");
            map.put("notice", "修改成功");
            map.put("success", true);
        } catch (Exception e) {
            map.put("notice", e.getMessage());
            map.put("success", false);
        }
        return JSON.toJSONString(map);
    }

    @RequestMapping(value = { "updateVersion" }, method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public Object updateVersion(long taskId, Long version, String filename) throws ServletException, IOException {
        Map<String, Object> map = new HashMap<String, Object>();

        try {
            this.taskService.updateAttachment(taskId, filename, version);

            map.put("notice", "修改成功");
            map.put("success", true);
        } catch (Exception e) {
            map.put("notice", e.getMessage());
            map.put("success", false);
        }
        return JSON.toJSONString(map);
    }

}
