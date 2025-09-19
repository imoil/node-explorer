package com.example.treeapi.dto;

import java.util.List;

public class SearchResultDto {
    private String id;
    private String name;
    private String type;
    private List<NodeDto> path;

    public SearchResultDto(String id, String name, String type, List<NodeDto> path) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<NodeDto> getPath() { return path; }
    public void setPath(List<NodeDto> path) { this.path = path; }
}

