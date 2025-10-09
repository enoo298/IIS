package hr.algebra.server.controller;

import hr.algebra.server.service.SearchApiResponseService;
import hr.algebra.server.xml.XmlItem;
import hr.algebra.server.xml.XmlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/api/response",
        produces = MediaType.APPLICATION_XML_VALUE
)
public class SearchApiResponseController {

    private final SearchApiResponseService service;


    @GetMapping
    public XmlResponse getResponse() {
        return service.getResponse();
    }

    @GetMapping("/items")
    public XmlResponse getAllItems() {

        return service.getResponse();
    }

    @GetMapping("/items/{position}")
    public ResponseEntity<XmlResponse> getItem(@PathVariable int position) {
        return service.getItemByPosition(position)
                .map(it -> {

                    XmlResponse base = service.getResponse();
                    XmlResponse one = new XmlResponse();
                    one.setStatus(base.getStatus());
                    one.setRequestId(base.getRequestId());
                    one.setData(new java.util.ArrayList<>());
                    one.getData().add(it);
                    return ResponseEntity.ok(one);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping(value = "/items", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<Void> createItem(@RequestBody XmlItem item) {
        XmlItem created = service.createItem(item);
        return ResponseEntity
                .created(URI.create("/api/response/items/" + created.getPosition()))
                .build();
    }

    @PutMapping(value = "/items/{position}", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<Object> updateItem(@PathVariable int position, @RequestBody XmlItem item) {
        return service.updateItem(position, item)
                .map(x -> ResponseEntity.noContent().build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/status", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<Void> setStatus(@RequestBody StatusBody body) {
        if (body == null || body.status == null) return ResponseEntity.badRequest().build();
        service.updateStatus(body.status);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/request-id", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<Void> setRequestId(@RequestBody RequestIdBody body) {
        if (body == null || body.requestId == null) return ResponseEntity.badRequest().build();
        service.updateRequestId(body.requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{position}")
    public ResponseEntity<Void> deleteItem(@PathVariable int position) {
        return service.deleteItem(position)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }


    @jakarta.xml.bind.annotation.XmlRootElement(name = "status")
    public static class StatusBody {
        public String status;
    }

    @jakarta.xml.bind.annotation.XmlRootElement(name = "requestId")
    public static class RequestIdBody {
        public String requestId;
    }
}
