// XmlResponse.java  (root element za spremljenu datoteku)
package hr.algebra.client.xml;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SearchResponse", namespace = "http://iis.com/search")

@XmlType(propOrder = {"status", "requestId", "data"})
public class XmlResponse {

    @XmlElement(namespace = "http://iis.com/search", required = true)
    private String status;


    @XmlElement(name = "request_id", namespace = "http://iis.com/search", required = true)
    private String requestId;

    @XmlElementWrapper(name = "data", namespace = "http://iis.com/search")
    @XmlElement(name = "item", namespace = "http://iis.com/search")
    private List<XmlItem> data = new ArrayList<>();


    public void setStatus(String status) {
        this.status = status;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setData(List<XmlItem> data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }

    public List<XmlItem> getData() {
        return data;
    }
}
