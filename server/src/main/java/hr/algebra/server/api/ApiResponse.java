package hr.algebra.server.api;

import lombok.Data;

import java.util.List;
@Data
public class ApiResponse {
    private String status;
    private String request_id;
    private List<ApiItem> data;

}