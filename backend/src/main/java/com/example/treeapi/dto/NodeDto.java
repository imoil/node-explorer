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

    public NodeDto() {
    }

    // Constructor to convert a Node entity to a NodeDto
    public NodeDto(com.example.treeapi.domain.Node entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.type = entity.getType();
        this.metadata = entity.getMetadata();
        this.hasChildren = entity.getHasChildren();
        if (entity.getSensors() != null) {
            this.sensors = entity.getSensors().stream()
                .map(NodeDto::fromSensorEntity)
                .collect(java.util.stream.Collectors.toList());
        }
    }

    // Factory method for conversion
    public static NodeDto fromEntity(com.example.treeapi.domain.Node entity) {
        return new NodeDto(entity);
    }

    // Private helper to convert Sensor entity to a NodeDto representation
    private static NodeDto fromSensorEntity(com.example.treeapi.domain.Sensor sensorEntity) {
        NodeDto dto = new NodeDto();
        dto.setId(sensorEntity.getId());
        dto.setName(sensorEntity.getName());
        dto.setType("sensor");
        dto.setMetadata(sensorEntity.getMetadata());
        dto.setHasChildren(false);
        return dto;
    }


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

