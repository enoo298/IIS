package hr.algebra.client.service;

import hr.algebra.client.xml.XmlItem;
import hr.algebra.client.xml.XmlResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class SearchResponseApi {

    private final String baseUrl;
    private final HttpClient client;
    private final AuthService auth;
    private final JAXBContext jaxb;

    public SearchResponseApi(AuthService auth) {
        this(auth, "http://localhost:8080");
    }

    public SearchResponseApi(AuthService auth, String baseUrl) {
        if (auth == null) throw new IllegalArgumentException("AuthService je obavezan.");
        this.auth = auth;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        try {
            this.jaxb = JAXBContext.newInstance(XmlResponse.class, XmlItem.class);
        } catch (Exception e) {
            throw new RuntimeException("Ne mogu inicijalizirati JAXBContext: " + e.getMessage(), e);
        }
    }



    public XmlResponse getResponse() {
        String xml = sendWithAuth(() -> baseGet("/api/response"));
        return unmarshal(XmlResponse.class, xml);
    }


    public XmlResponse getAllItems() {
        String xml = sendWithAuth(() -> baseGet("/api/response/items"));
        return unmarshal(XmlResponse.class, xml);
    }

    /** GET /api/response/items/{position} -> mini-XmlResponse s jednim itemom, 404 ako nema */
    public Optional<XmlItem> getItem(int position) {
        HttpResponse<String> res = sendWithAuthRaw(() -> baseGet("/api/response/items/" + position));
        if (res.statusCode() == 404) return Optional.empty();
        if (res.statusCode() < 200 || res.statusCode() >= 300)
            throw new RuntimeException("GET item HTTP status: " + res.statusCode() + "\n" + res.body());

        XmlResponse r = unmarshal(XmlResponse.class, res.body());
        return (r.getData() != null && !r.getData().isEmpty()) ? Optional.of(r.getData().get(0)) : Optional.empty();
    }


    public String createItem(XmlItem item) {
        String body = marshal(item);
        HttpResponse<String> res = sendWithAuthRaw(() -> basePost("/api/response/items", body));
        if (res.statusCode() != 201)
            throw new RuntimeException("POST item HTTP status: " + res.statusCode() + "\n" + res.body());
        return res.headers().firstValue("Location").orElse("");
    }


    public boolean updateItem(int position, XmlItem item) {
        String body = marshal(item);
        HttpResponse<String> res = sendWithAuthRaw(() -> basePut("/api/response/items/" + position, body));
        if (res.statusCode() == 204) return true;
        if (res.statusCode() == 404) return false;
        throw new RuntimeException("PUT item HTTP status: " + res.statusCode() + "\n" + res.body());
    }


    public boolean deleteItem(int position) {
        HttpResponse<String> res = sendWithAuthRaw(() -> baseDelete("/api/response/items/" + position));
        if (res.statusCode() == 204) return true;
        if (res.statusCode() == 404) return false;
        throw new RuntimeException("DELETE item HTTP status: " + res.statusCode() + "\n" + res.body());
    }







    private HttpRequest.Builder baseGet(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/xml");
    }

    private HttpRequest.Builder basePost(String path, String bodyXml) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "*/*") // server vraća 201 bez body-ja
                .header("Content-Type", "application/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(bodyXml, StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder basePut(String path, String bodyXml) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "*/*") // 204 No Content
                .header("Content-Type", "application/xml; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(bodyXml, StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder baseDelete(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "*/*")
                .DELETE();
    }

    private HttpRequest withAuth(HttpRequest.Builder b) {
        String token = auth.getValidAccessToken();
        if (token == null || token.isBlank())
            throw new IllegalStateException("Niste prijavljeni ili je token nedostupan.");
        return b.header("Authorization", "Bearer " + token).build();
    }


    private String sendWithAuth(Supplier<HttpRequest.Builder> supplier) {
        return sendWithAuthRaw(supplier).body();
    }


    private HttpResponse<String> sendWithAuthRaw(Supplier<HttpRequest.Builder> supplier) {
        try {

            HttpRequest req = withAuth(supplier.get());
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() == 401 || res.statusCode() == 403) {
                HttpRequest retry = withAuth(supplier.get());
                return client.send(retry, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            }
            return res;
        } catch (Exception e) {
            throw new RuntimeException("HTTP greška: " + e.getMessage(), e);
        }
    }



    private String marshal(Object obj) {
        try {
            StringWriter sw = new StringWriter();
            Marshaller m = jaxb.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(obj, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("JAXB marshal greška: " + e.getMessage(), e);
        }
    }

    private <T> T unmarshal(Class<T> type, String xml) {
        try {
            Unmarshaller um = jaxb.createUnmarshaller();
            return type.cast(um.unmarshal(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException("JAXB unmarshal greška: " + e.getMessage(), e);
        }
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
