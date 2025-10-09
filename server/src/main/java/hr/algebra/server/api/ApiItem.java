package hr.algebra.server.api;


import lombok.Data;

@Data
public class ApiItem {
    private String title;
    private String snippet;
    private String url;
    private String domain;
    private int position;
}