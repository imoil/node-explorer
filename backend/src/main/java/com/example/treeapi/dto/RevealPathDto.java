package com.example.treeapi.dto;

import java.util.List;
import java.util.Map;

public class RevealPathDto {

    private List<NodeDto> path;
    private Map<String, List<NodeDto>> childrenMap;

    public RevealPathDto(List<NodeDto> path, Map<String, List<NodeDto>> childrenMap) {
        this.path = path;
        this.childrenMap = childrenMap;
    }

    // Getters and Setters
    public List<NodeDto> getPath() {
        return path;
    }

    public void setPath(List<NodeDto> path) {
        this.path = path;
    }

    public Map<String, List<NodeDto>> getChildrenMap() {
        return childrenMap;
    }

    public void setChildrenMap(Map<String, List<NodeDto>> childrenMap) {
        this.childrenMap = childrenMap;
    }
}
