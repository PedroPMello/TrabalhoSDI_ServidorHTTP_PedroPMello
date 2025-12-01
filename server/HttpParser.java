package server;

import java.util.HashMap;
import java.util.Map;

public class HttpParser {
    private Map<String, String> headers = new HashMap<>();
    private String method;
    private String path;
    private String httpVersion;
    private String body;
    public HttpParser() {
        headers = new HashMap<>();
    }
    //getters and setters
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getHttpVersion() {
        return httpVersion;
    }
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public String getHeader(String key) {
        return this.headers.get(key);
    }
    public void setHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(path).append(" ").append(httpVersion).append("\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n").append(body);
        return sb.toString();
    }
}