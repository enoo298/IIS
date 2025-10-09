package hr.algebra.server.soap;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"results", "validationMessage"})
@XmlRootElement(name = "SearchResponse", namespace = "http://iis.com/search")
public class SearchResponse {

    @XmlElement(name="result", namespace = "http://iis.com/search")
    private List<Result> results = new ArrayList<>();

    @XmlElement(namespace = "http://iis.com/search")
    private List<String> validationMessage = new ArrayList<>();

    public List<Result> getResults() { return results; }
    public List<String> getValidationMessage() { return validationMessage; }
}
