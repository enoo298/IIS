package hr.algebra.client.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthService {

    private String accessToken;
    private String refreshToken;
    private Instant accessTokenExpiration;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public boolean login(String username, String password) {
        try {
            LoginRequest requestBody = new LoginRequest(username, password);
            String json = mapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                AuthResponse auth = mapper.readValue(response.body(), AuthResponse.class);
                this.accessToken = auth.getAccessToken();
                this.refreshToken = auth.getRefreshToken();
                this.accessTokenExpiration = parseJwtExpiry(accessToken);
                return true;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized String getValidAccessToken() {
        if (accessToken == null || accessTokenExpiration == null || Instant.now().isAfter(accessTokenExpiration.minusSeconds(5))) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() {
        try {
            RefreshRequest refreshBody = new RefreshRequest(refreshToken);
            String json = mapper.writeValueAsString(refreshBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/refresh"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                AuthResponse auth = mapper.readValue(response.body(), AuthResponse.class);
                this.accessToken = auth.getAccessToken();
                this.accessTokenExpiration = parseJwtExpiry(accessToken);

            } else {
                throw new RuntimeException("Nevažeći refresh token. Molimo ponovno se prijavite.");
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Greška kod osvježavanja tokena: " + e.getMessage());
        }
    }

    private Instant parseJwtExpiry(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Pattern pattern = Pattern.compile("\"exp\":(\\d+)");
            Matcher matcher = pattern.matcher(payload);
            if (matcher.find()) {
                long exp = Long.parseLong(matcher.group(1));
                return Instant.ofEpochSecond(exp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Data
    static class LoginRequest {
        private final String username;
        private final String password;
    }

    @Data
    static class RefreshRequest {
        private final String refreshToken;
    }

    @Data
    static class AuthResponse {
        private String accessToken;
        private String refreshToken;
    }
}

