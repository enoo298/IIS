package hr.algebra.client.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class SoapService {

    private static final String SOAP_URL = "http://localhost:8080/ws";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final AuthService authService;

    public SoapService(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("AuthService je obavezan (Authorization nije opcionalan).");
        }
        this.authService = authService;
    }


    public CompletableFuture<String> searchSoapAsync(String keyword, int limit) {
        String body = buildEnvelope(keyword, limit);


        HttpRequest request = buildAuthorizedRequest(body, authService.getValidAccessToken());


        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(resp -> {
                    if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                        String refreshed = authService.getValidAccessToken();
                        HttpRequest retry = buildAuthorizedRequest(body, refreshed);
                        return client.sendAsync(retry, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    }
                    return CompletableFuture.completedFuture(resp);
                })
                .thenApply(resp -> {
                    int sc = resp.statusCode();
                    if (sc >= 200 && sc < 300) return resp.body();
                    throw new RuntimeException("SOAP HTTP status: " + sc + "\n" + resp.body());
                });
    }


    public String searchSoap(String keyword, int limit) {
        return searchSoapAsync(keyword, limit).join();
    }



    private HttpRequest buildAuthorizedRequest(String body, String bearer) {
        if (bearer == null || bearer.isBlank()) {
            throw new IllegalStateException("Niste prijavljeni ili je token nedostupan.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(SOAP_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("Accept", "text/xml")
                .header("Authorization", "Bearer " + bearer)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    private String buildEnvelope(String keyword, int limit) {
        if (keyword == null || keyword.isBlank())
            throw new IllegalArgumentException("keyword je obavezan.");
        if (limit <= 0)
            throw new IllegalArgumentException("limit mora biti > 0.");

        String kw = xmlEscape(keyword);

        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:sr="http://iis.com/search">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <sr:SearchRequest>
                      <sr:keyword>%s</sr:keyword>
                      <sr:limit>%d</sr:limit>
                    </sr:SearchRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(kw, limit);
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
