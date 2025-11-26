package com.roomruster.notify;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DiscordNotifier {
    private final String webhookUrl;
    private final HttpClient client;

    public DiscordNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void send(String message) throws Exception {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Discord webhook URL is not set");
        }
        
        // Discord webhook expects JSON: {"content":"..."}
        String payload = String.format("{\"content\":\"%s\"}", 
            message.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
        );
        
        System.out.println("Sending to Discord: " + payload);
        
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
                    
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            
            System.out.println("Discord webhook response: " + code + " - " + resp.body());
            
            if (code < 200 || code >= 300) {
                throw new RuntimeException("Discord webhook failed: HTTP " + code + " - " + resp.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending to Discord: " + e.getMessage());
            throw e;
        }
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        
        sb.append('"');
        return sb.toString();
    }
}
