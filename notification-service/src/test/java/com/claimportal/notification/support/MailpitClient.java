package com.claimportal.notification.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/** Minimal client for Mailpit's REST API, used to assert on captured emails. */
public class MailpitClient {

    public record CapturedEmail(String id, String subject, String to) {
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String baseUrl;

    public MailpitClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<CapturedEmail> messages() {
        JsonNode root = json.readTree(get("/api/v1/messages"));
        List<CapturedEmail> emails = new ArrayList<>();
        for (JsonNode m : root.get("messages")) {
            emails.add(new CapturedEmail(m.get("ID").asString(), m.get("Subject").asString(),
                    m.get("To").get(0).get("Address").asString()));
        }
        return emails;
    }

    public String header(String messageId, String name) {
        JsonNode headers = json.readTree(get("/api/v1/message/" + messageId + "/headers"));
        JsonNode values = headers.get(name);
        return values == null ? null : values.get(0).asString();
    }

    public void deleteAll() {
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/messages")).DELETE().build());
    }

    private String get(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build());
    }

    private String send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            throw new IllegalStateException("Mailpit request failed: " + request.uri(), e);
        }
    }
}
