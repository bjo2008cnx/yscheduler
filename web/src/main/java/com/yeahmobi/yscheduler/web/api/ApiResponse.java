package com.yeahmobi.yscheduler.web.api;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Leo Liang
 */
@Data
public class ApiResponse {

    private ApiStatusCode status = ApiStatusCode.SUCCESS;
    private String message;
    private Map<String, Object> returnValue = new HashMap<String, Object>();

    public void addReturnValue(String key, Object value) {
        this.returnValue.put(key, value);
    }

    public boolean isSuccess() {
        return ApiStatusCode.SUCCESS.equals(this.status);
    }
}
