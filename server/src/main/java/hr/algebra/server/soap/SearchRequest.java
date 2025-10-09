package hr.algebra.server.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"keyword", "limit"})
@XmlRootElement(name = "SearchRequest", namespace = "http://iis.com/search")
public class SearchRequest {
    @XmlElement(namespace = "http://iis.com/search", required = true)
    private String keyword;

    @XmlElement(namespace = "http://iis.com/search")
    private Integer limit;

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getKeyword() {
        return keyword;
    }

    public Integer getLimit() {
        return limit;
    }


}
