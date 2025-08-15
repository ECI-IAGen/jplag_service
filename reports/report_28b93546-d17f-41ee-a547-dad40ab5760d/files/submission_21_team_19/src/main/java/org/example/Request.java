package org.example;

import java.util.HashMap;
import java.util.Map;

public class Request {

    private final String method;
    private final String path;
    private String body;

    private final Map<String, String> queryParams;
    
    public Request (String method, String path, String body, String query){
        this.method = method;
        this.path  = path;
        this.body = body;
        this.queryParams = parseQuery(query);
    }



    public Request (String method, String path){
        this.method = method;
        this.path  = path;
        this.body = null;
        this.queryParams = null;
    }

    public Request (String method, String path, String body){
        this.method = method;
        this.path  = path;
        this.body = body;
        this.queryParams = null;
    }

    private Map<String, String> parseQuery(String query) {

        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    public String getMethod(){
        return method;
    }

    public String getPath(){
        return path;
    }

    public String getBody() {
        return body;
    }

    public String getValues(String key) {
        return queryParams.getOrDefault(key, null);
    }
    public void setQueryParams(String key, String value) {
        this.queryParams.put(key, value);
    }
}
