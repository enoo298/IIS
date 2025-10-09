package hr.algebra.server.controller;

import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import hr.algebra.server.handler.XmlErrorHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/xml")
public class XmlValidationController {

    @PostMapping(value = "/xsd", consumes = "application/xml")
    public ResponseEntity<String> validateWithXsd(@RequestBody String xml) {
        try {
            InputStream xsdStream = getClass().getClassLoader().getResourceAsStream("schema/searchResults.xsd");
            if (xsdStream == null) {
                return ResponseEntity.internalServerError().body("XSD shema nije pronađena.");
            }

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();

            XmlErrorHandler errorHandler = new XmlErrorHandler();
            validator.setErrorHandler(errorHandler);
            validator.validate(new StreamSource(new StringReader(xml)));

            if (!errorHandler.getExceptions().isEmpty()) {
                StringBuilder sb = new StringBuilder("Greške u XSD validaciji:\n");
                for (SAXParseException ex : errorHandler.getExceptions()) {
                    sb.append("- ").append(ex.getMessage()).append("\n");
                }
                return ResponseEntity.badRequest().body(sb.toString());
            }

            saveXmlToFileIfNotExists(xml, "saved_xsd.xml");

            return ResponseEntity.ok("XML validan prema XSD shemi!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Greška pri XSD validaciji: " + e.getMessage());
        }
    }

    @PostMapping(value = "/rng", consumes = "application/xml")
    public ResponseEntity<String> uploadXmlAndValidateWithRng(@RequestBody String xmlContent) {
        try {
            var rngSchema = getClass().getClassLoader().getResourceAsStream("schema/searchResults.rng");
            if (rngSchema == null) {
                return ResponseEntity.internalServerError().body("RNG shema nije pronađena.");
            }

            var errorHandler = new XmlErrorHandler();
            var builder = new PropertyMapBuilder();
            builder.put(ValidateProperty.ERROR_HANDLER, errorHandler);

            var driver = new ValidationDriver(builder.toPropertyMap());
            driver.loadSchema(new InputSource(rngSchema));
            boolean valid = driver.validate(new InputSource(new StringReader(xmlContent)));

            if (!errorHandler.getExceptions().isEmpty() || !valid) {
                StringBuilder sb = new StringBuilder("Greške u RNG validaciji:\n");
                for (var ex : errorHandler.getExceptions()) {
                    sb.append("- ").append(ex.getMessage()).append("\n");
                }
                if (errorHandler.getExceptions().isEmpty() && !valid) {
                    sb.append("- XML nije validan prema shemi.\n");
                }
                return ResponseEntity.badRequest().body(sb.toString());
            }

            saveXmlToFileIfNotExists(xmlContent, "saved_rng.xml");

            return ResponseEntity.ok("XML validan prema RNG shemi!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Greška pri validaciji: " + e.getMessage());
        }
    }

    private void saveXmlToFileIfNotExists(String xmlContent, String filename) throws Exception {

        Path path = Path.of("src/main/resources/schema", filename);

        Files.createDirectories(path.getParent());

        if (Files.exists(path)) {
            String existingContent = Files.readString(path, StandardCharsets.UTF_8);


            if (existingContent.equals(xmlContent)) {
                System.out.println("XML je već spremljen i nije se promijenio: " + path.getFileName());
                return;
            }


            System.out.println("XML se razlikuje – prepisujem: " + path.getFileName());
        } else {
            System.out.println("XML još ne postoji – spremam: " + path.getFileName());
        }

        Files.writeString(path, xmlContent, StandardCharsets.UTF_8);
    }

}
