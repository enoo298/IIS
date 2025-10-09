package hr.algebra.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.algebra.server.api.ApiItem;
import hr.algebra.server.api.ApiResponse;
import hr.algebra.server.xml.XmlItem;
import hr.algebra.server.xml.XmlResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class SearchApiService {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${rapidapi.url}")
    private String apiUrl;

    @Value("${rapidapi.host}")
    private String apiHost;

    @Value("${rapidapi.key}")
    private String apiKey;

    @Value("${rapidapi.default-limit:10}")
    private int defaultLimit;

    @Value("${app.search.xml.path}")
    private String xmlPathStr;

    @Value("${app.search.xml.xsd}")
    private Resource xmlXsd;

    public File saveSearchResultsToFile(String keyword, Integer limitOpt) throws Exception {
        int limit = (limitOpt != null ? limitOpt : defaultLimit);


        String url = apiUrl + "?q=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) +
                "&limit=" + limit;

        HttpHeaders h = new HttpHeaders();
        h.set("x-rapidapi-host", apiHost);
        h.set("x-rapidapi-key", apiKey);

        HttpEntity<Void> req = new HttpEntity<>(h);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("RapidAPI error: " + resp.getStatusCode());
        }


        ApiResponse api = mapper.readValue(resp.getBody(), ApiResponse.class);
        List<ApiItem> items = api.getData();


        XmlResponse xml = new XmlResponse();
        xml.setStatus(api.getStatus());
        xml.setRequestId(api.getRequest_id());
        for (ApiItem it : items) {
            XmlItem xi = new XmlItem();
            xi.setTitle(it.getTitle());
            xi.setSnippet(it.getSnippet());
            xi.setUrl(it.getUrl());
            xi.setDomain(it.getDomain());
            xi.setPosition(it.getPosition());
            xml.getData().add(xi);
        }


        Path xmlPath = Path.of(xmlPathStr).toAbsolutePath();
        Files.createDirectories(xmlPath.getParent());
        JAXBContext ctx = JAXBContext.newInstance(XmlResponse.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        try (OutputStream os = Files.newOutputStream(xmlPath)) {
            m.marshal(xml, os);
        }

        validateXmlAgainstXsd(xmlPath.toFile(), xmlXsd.getInputStream());

        return xmlPath.toFile();
    }

    public void validateXmlAgainstXsd(File xmlFile, InputStream xsdStream) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(xmlFile));
    }

    public String loadPreparedXml() throws IOException {
        Path xmlPath = Path.of(xmlPathStr).toAbsolutePath();
        if (!Files.exists(xmlPath)) return null;
        return Files.readString(xmlPath);
    }
}
