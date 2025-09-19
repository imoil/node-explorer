package com.example.treeapi.dto;

import java.util.List;
import java.util.Map;

public class NodeDto {
    private String id;
    private String name;
    private String type;
    private Map<String, String> metadata;
    private List<NodeDto> sensors;
    private boolean hasChildren;

    // ========================[ 변경된 부분 시작 ]========================
    /**
     * 기본 생성자: Jackson 라이브러리가 JSON을 객체로 변환할 때 사용합니다.
     */
    public NodeDto() {
    }

    /**
     * 센서 노드를 생성하기 위한 생성자입니다.
     */
    public NodeDto(String id, String name, Map<String, String> metadata) {
        this.id = id;
        this.name = name;
        this.type = "sensor";
        this.metadata = metadata;
        this.hasChildren = false; // 센서는 자식을 가질 수 없습니다.
    }

    /**
     * 폴더 노드를 생성하기 위한 생성자입니다.
     */
    public NodeDto(String id, String name, boolean hasChildren, Map<String, String> metadata) {
        this.id = id;
        this.name = name;
        this.type = "folder";
        this.hasChildren = hasChildren;
        this.metadata = metadata;
    }
    // ========================[ 변경된 부분 끝 ]========================


    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<NodeDto> getSensors() {
        return sensors;
    }

    public void setSensors(List<NodeDto> sensors) {
        this.sensors = sensors;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }
}

