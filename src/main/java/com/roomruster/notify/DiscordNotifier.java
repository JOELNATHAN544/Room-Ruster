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

    public void send(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            String error = "ERROR: Discord webhook URL is not set";
            System.err.println(error);
            throw new IllegalArgumentException(error);
        }
        
        try {
            // Discord webhook expects JSON: {"content":"..."}
            String payload = String.format("{\"content\":\"%s\"}", 
                message.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t")
            );
            
            System.out.println("Sending to Discord, payload length: " + payload.length());
            
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
                    
            System.out.println("Sending HTTP request to Discord...");
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            String responseBody = resp.body();
            
            System.out.println("Discord webhook response status: " + code);
            System.out.println("Response body: " + responseBody);
            
            if (code < 200 || code >= 300) {
                String error = "Discord webhook failed: HTTP " + code + " - " + responseBody;
                System.err.println(error);
                throw new RuntimeException(error);
            }
        } catch (Exception e) {
            String error = "Error sending to Discord: " + e.getMessage();
            System.err.println(error);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(error, e);
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
