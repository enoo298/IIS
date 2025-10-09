package hr.algebra.client.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class XmlValidationService {

    private final String baseUrl;
    private final HttpClient client;
    private final AuthService authService;

    public XmlValidationService(AuthService authService) {
        this("http://localhost:8080", authService);
    }

    public XmlValidationService(String baseUrl, AuthService authService) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.authService = authService;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Result validateWithXsd(String xml) {
        return postXml("/api/xml/xsd", xml);
    }

    public Result validateWithRng(String xml) {
        return postXml("/api/xml/rng", xml);
    }



    private Result postXml(String path, String xmlBody) {
        try {
            String token = authService.getValidAccessToken();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .header("Accept", "text/plain; charset=UTF-8")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(xmlBody == null ? "" : xmlBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() == 401) {
                String refreshed = authService.getValidAccessToken();
                HttpRequest retry = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/xml; charset=UTF-8")
                        .header("Accept", "text/plain; charset=UTF-8")
                        .header("Authorization", "Bearer " + refreshed)
                        .POST(HttpRequest.BodyPublishers.ofString(xmlBody == null ? "" : xmlBody, StandardCharsets.UTF_8))
                        .build();
                res = client.send(retry, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            }

            boolean ok = res.statusCode() >= 200 && res.statusCode() < 300;
            return new Result(ok, res.statusCode(), res.body());

        } catch (IOException e) {
            return new Result(false, 0, "Greška pri slanju zahtjeva: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, 0, "Zahtjev prekinut.");
        }
    }

    private static String readFile(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Ne mogu pročitati XML iz: " + p + " -> " + e.getMessage(), e);
        }
    }



    public record Result(boolean ok, int status, String message) {}
}
