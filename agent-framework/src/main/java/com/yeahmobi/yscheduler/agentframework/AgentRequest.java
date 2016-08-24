/**
 *
 */
package com.yeahmobi.yscheduler.agentframework;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Leo.Liang
 */
@Data
@AllArgsConstructor
public class AgentRequest {

    public static final String  REQKEY_EVENT_TYPE = "eventType";
    public static final String  REQKEY_PARAMS     = "params";

    private String              eventType;
    private Map<String, String> params;

    @SuppressWarnings("unchecked")
    public static AgentRequest valueOf(HttpServletRequest req) {
        // 从queryString和params json value中获取用户key-value参数（需要排除系统的保留参数：params和eventType）
        Map<String, String> jsonParams = JSON.parseObject(req.getParameter(REQKEY_PARAMS),
                                                          new TypeReference<Map<String, String>>() {
                                                          }.getType());
        if (jsonParams == null) {
            jsonParams = new HashMap<String, String>();
        }
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            jsonParams.put(name, req.getParameter(name));
        }
        jsonParams.remove(REQKEY_EVENT_TYPE);
        jsonParams.remove(REQKEY_PARAMS);

        return new AgentRequest(req.getParameter(REQKEY_EVENT_TYPE), jsonParams);
    }
}
