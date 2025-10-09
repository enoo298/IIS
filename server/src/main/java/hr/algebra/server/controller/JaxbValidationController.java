package hr.algebra.server.controller;

import hr.algebra.server.xml.XmlResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/jaxb")
public class JaxbValidationController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> validateApiXml() {
        List<String> errors = new ArrayList<>();
        try {
            JAXBContext ctx = JAXBContext.newInstance(XmlResponse.class);
            Unmarshaller um = ctx.createUnmarshaller();


            var xsdRes = new ClassPathResource("schema/searchApiResponse.xsd");
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(xsdRes.getURL());
            um.setSchema(schema);


            um.setEventHandler(ev -> { errors.add(ev.getMessage()); return true; });

            Resource xmlRes = new FileSystemResource("data/search_api_response.xml");
            if (!xmlRes.exists()) {
                xmlRes = new ClassPathResource("schema/search_api_response.xml");
            }

            try (InputStream is = xmlRes.getInputStream()) {
                um.unmarshal(is);
            }

        } catch (Exception e) {
            errors.add("Greška: " + e.getMessage());
        }
        return errors.isEmpty()
                ? List.of("XML je validan prema searchApiResponse.xsd.")
                : errors;
    }
}
