package hr.algebra.server.soap;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResultType", propOrder = {"title","snippet","url","domain","position"})
public class Result {
    @XmlElement(namespace = "http://iis.com/search", required = true)
    private String title;
    @XmlElement(namespace = "http://iis.com/search", required = true)
    private String snippet;
    @XmlElement(namespace = "http://iis.com/search", required = true)
    private String url;
    @XmlElement(namespace = "http://iis.com/search", required = true)
    private String domain;
    @XmlElement(namespace = "http://iis.com/search", required = true)
    private int position;


    public void setTitle(String title) {
        this.title = title;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getUrl() {
        return url;
    }

    public String getDomain() {
        return domain;
    }

    public int getPosition() {
        return position;
    }
}
