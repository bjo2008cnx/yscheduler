package com.yeahmobi.yscheduler.web.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
//@ToString
public class ApiRequest {

    private String appKey;
    private String token;

    @Override
    public String toString() {
        return "ApiRequest [appKey=" + this.appKey + ", token=" + this.token + "]";
    }

}
