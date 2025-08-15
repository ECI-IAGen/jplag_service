package edu.escuelaing.arep.ASE.app.http;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    private Map<String, String> queryParams;

    public Request(String method, String path) {
        this.method = method;
        this.path = path;
        this.queryParams = new HashMap<>();
    }

    public void addQueryParam(String key, String value) {
        queryParams.put(key, value);
    }

    public String getValues(String key) {
        return queryParams.get(key);
    }
}
