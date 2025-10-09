package hr.algebra.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class JaxbValidationApi {

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthService authService;

    public JaxbValidationApi(AuthService authService) {
        this("http://localhost:8080", authService);
    }

    public JaxbValidationApi(String baseUrl, AuthService authService) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.authService = authService;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Result validate() {
        try {
            String token = authService.getValidAccessToken();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/jaxb"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());


            if (res.statusCode() == 401) {
                String refreshed = authService.getValidAccessToken();
                HttpRequest retry = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/jaxb"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .header("Authorization", "Bearer " + refreshed)
                        .GET()
                        .build();
                res = client.send(retry, HttpResponse.BodyHandlers.ofString());
            }

            List<String> messages = mapper.readValue(res.body(), new TypeReference<List<String>>() {});
            boolean ok = res.statusCode() >= 200 && res.statusCode() < 300
                    && messages.size() == 1
                    && messages.get(0).toLowerCase().contains("xml je validan");

            return new Result(ok, res.statusCode(), messages);

        } catch (Exception e) {
            return new Result(false, 0, List.of("Greška pri pozivu /api/jaxb: " + e.getMessage()));
        }
    }

    public record Result(boolean valid, int status, List<String> messages) {}
}
