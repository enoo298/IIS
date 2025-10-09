package hr.algebra.server.service;

import hr.algebra.server.xml.XmlItem;
import hr.algebra.server.xml.XmlResponse;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class SearchApiResponseService {

    private static final Path DATA_PATH = Paths.get("data", "search_api_response.xml");


    private static final String TEMPLATE_CLASSPATH = "data/search_api_response.xml";


    private static final String XSD_CLASSPATH = "schema/searchApiResponse.xsd";

    private final Object lock = new Object();
    private XmlResponse response;


    private volatile long lastLoadedMtime = -1L;

    @PostConstruct
    public void init() {
        loadFromDisk();
    }



    public XmlResponse getResponse() {
        ensureFresh();
        synchronized (lock) {
            return response;
        }
    }

    public List<XmlItem> getAllItems() {
        ensureFresh();
        synchronized (lock) {
            var list = response.getData();
            return (list == null) ? List.of() : new ArrayList<>(list);
        }
    }

    public Optional<XmlItem> getItemByPosition(int position) {
        ensureFresh();
        synchronized (lock) {
            return response.getData() == null ? Optional.empty()
                    : response.getData().stream().filter(i -> i.getPosition() == position).findFirst();
        }
    }

    public XmlItem createItem(XmlItem item) {
        ensureFresh();
        synchronized (lock) {
            ensureItemsList();

            int pos = item.getPosition();
            if (pos <= 0) {
                pos = nextPosition();
                item.setPosition(pos);
            } else if (existsPosition(pos)) {
                throw new IllegalArgumentException("Item s position=" + pos + " već postoji.");
            }

            response.getData().add(item);
            saveToDisk();
            return item;
        }
    }

    public Optional<XmlItem> updateItem(int position, XmlItem incoming) {
        ensureFresh();
        synchronized (lock) {
            ensureItemsList();

            for (int i = 0; i < response.getData().size(); i++) {
                XmlItem current = response.getData().get(i);
                if (current.getPosition() == position) {
                    incoming.setPosition(position);
                    response.getData().set(i, incoming);
                    saveToDisk();
                    return Optional.of(incoming);
                }
            }
            return Optional.empty();
        }
    }

    public boolean deleteItem(int position) {
        ensureFresh();
        synchronized (lock) {
            ensureItemsList();
            boolean removed = response.getData().removeIf(i -> i.getPosition() == position);
            if (removed) saveToDisk();
            return removed;
        }
    }

    public void updateStatus(String status) {
        ensureFresh();
        synchronized (lock) {
            response.setStatus(status);
            saveToDisk();
        }
    }

    public void updateRequestId(String requestId) {
        ensureFresh();
        synchronized (lock) {
            response.setRequestId(requestId);
            saveToDisk();
        }
    }





    private void loadFromDisk() {
        try {
            File file = ensureDataFileExists();
            JAXBContext ctx = JAXBContext.newInstance(XmlResponse.class, XmlItem.class);
            Unmarshaller um = ctx.createUnmarshaller(); // validaciju radimo pri spremanju
            XmlResponse loaded = (XmlResponse) um.unmarshal(file);
            if (loaded.getData() == null) {
                loaded.setData(new ArrayList<>());
            }
            synchronized (lock) {
                this.response = loaded;
                this.lastLoadedMtime = Files.getLastModifiedTime(DATA_PATH).toMillis();
            }
        } catch (Exception e) {
            throw new RuntimeException("Učitavanje XML-a nije uspjelo: " + e.getMessage(), e);
        }
    }


    private void ensureFresh() {
        try {
            long m = Files.getLastModifiedTime(DATA_PATH).toMillis();
            if (m != lastLoadedMtime) {
                synchronized (lock) {
                    long m2 = Files.getLastModifiedTime(DATA_PATH).toMillis();
                    if (m2 != lastLoadedMtime) {
                        JAXBContext ctx = JAXBContext.newInstance(XmlResponse.class, XmlItem.class);
                        Unmarshaller um = ctx.createUnmarshaller();
                        XmlResponse fresh = (XmlResponse) um.unmarshal(DATA_PATH.toFile());
                        if (fresh.getData() == null) fresh.setData(new ArrayList<>());
                        this.response = fresh;
                        this.lastLoadedMtime = m2;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Reload XML-a nije uspio: " + e.getMessage(), e);
        }
    }

    private File ensureDataFileExists() throws Exception {
        Files.createDirectories(DATA_PATH.getParent());
        if (!Files.exists(DATA_PATH)) {
            ClassPathResource cp = new ClassPathResource(TEMPLATE_CLASSPATH);
            if (cp.exists()) {
                try (InputStream in = cp.getInputStream()) {
                    Files.copy(in, DATA_PATH);
                }
            } else {
                XmlResponse empty = new XmlResponse();
                empty.setStatus("OK");
                empty.setRequestId(UUID.randomUUID().toString());
                empty.setData(new ArrayList<>());

                JAXBContext ctx = JAXBContext.newInstance(XmlResponse.class, XmlItem.class);
                Marshaller m = ctx.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                m.marshal(empty, DATA_PATH.toFile());
            }
        }
        return DATA_PATH.toFile();
    }

    private void saveToDisk() {
        try {
            JAXBContext ctx = JAXBContext.newInstance(XmlResponse.class, XmlItem.class);
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setSchema(loadSchema());


            Path tmp = DATA_PATH.resolveSibling(DATA_PATH.getFileName() + ".tmp");
            m.marshal(response, tmp.toFile());
            try {
                Files.move(tmp, DATA_PATH,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception notAtomic) {
                Files.move(tmp, DATA_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            this.lastLoadedMtime = Files.getLastModifiedTime(DATA_PATH).toMillis();
        } catch (Exception e) {
            throw new RuntimeException("Spremanje XML-a nije uspjelo: " + e.getMessage(), e);
        }
    }

    private Schema loadSchema() throws Exception {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        ClassPathResource xsd = new ClassPathResource(XSD_CLASSPATH);
        return sf.newSchema(xsd.getURL());
    }

    private void ensureItemsList() {
        if (response.getData() == null) {
            response.setData(new ArrayList<>());
        }
    }

    private boolean existsPosition(int position) {
        return response.getData().stream().anyMatch(i -> i.getPosition() == position);
    }

    private int nextPosition() {
        return response.getData().stream()
                .mapToInt(XmlItem::getPosition)
                .max()
                .orElse(0) + 1;
    }
}
