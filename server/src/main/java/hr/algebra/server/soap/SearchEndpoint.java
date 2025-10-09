package hr.algebra.server.soap;

import hr.algebra.server.service.SearchApiService;
import org.springframework.ws.server.endpoint.annotation.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Endpoint
public class SearchEndpoint {

    private static final String NS = "http://iis.com/search";
    private final SearchApiService service;

    public SearchEndpoint(SearchApiService service) {
        this.service = service;
    }

    @PayloadRoot(namespace = NS, localPart = "SearchRequest")
    @ResponsePayload
    public SearchResponse search(@RequestPayload SearchRequest req) throws Exception {
        String keyword = (req.getKeyword() == null) ? "" : req.getKeyword().trim();
        Integer limit = req.getLimit();


        service.saveSearchResultsToFile(keyword, limit);


        String xml = service.loadPreparedXml();
        if (xml == null) throw new IllegalStateException("Prepared XML not found.");


        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        String lower = keyword.toLowerCase();
        String expr = "/*[local-name()='SearchResponse']/*[local-name()='data']/*[local-name()='item']" +
                "[contains(translate(*[local-name()='title'], 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + lower + "')]";

        NodeList nodes = (NodeList) xpath.evaluate(expr, doc, XPathConstants.NODESET);


        SearchResponse resp = new SearchResponse();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);

            Result r = new Result();
            r.setTitle(getText(item, "title"));
            r.setSnippet(getText(item, "snippet"));
            r.setUrl(getText(item, "url"));
            r.setDomain(getText(item, "domain"));
            r.setPosition(Integer.parseInt(getText(item, "position")));
            resp.getResults().add(r);
        }

        if (resp.getResults().isEmpty()) {
            resp.getValidationMessage().add("Nema rezultata za: " + keyword);
        }

        return resp;
    }

    private String getText(Element parent, String local) {
        NodeList nl = parent.getElementsByTagNameNS(NS, local);
        if (nl.getLength() == 0) nl = parent.getElementsByTagName(local);
        return (nl.getLength() > 0) ? nl.item(0).getTextContent() : "";
    }
}
