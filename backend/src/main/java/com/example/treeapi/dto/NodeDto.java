package com.example.treeapi.dto;

import com.example.treeapi.domain.Node;
import com.example.treeapi.domain.Sensor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeDto {
    private String id;
    private String name;
    private String type;
    private String parentId; // Re-add parentId
    private boolean hasChildren;
    private Map<String, String> metadata;
    private List<NodeDto> sensors; // Re-add sensors list

    public NodeDto() {
    }

    // Constructor to convert a Node entity to a NodeDto
    public NodeDto(Node entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.type = entity.getType();
        this.parentId = entity.getParentId(); // Initialize parentId
        this.hasChildren = entity.getHasChildren();
        this.metadata = entity.getMetadata();
        // Restore sensor conversion logic
        if (entity.getSensors() != null) {
            this.sensors = entity.getSensors().stream()
                .map(NodeDto::fromSensorEntity)
                .collect(Collectors.toList());
        }
    }

    // Factory method for conversion from Node
    public static NodeDto fromEntity(Node entity) {
        return new NodeDto(entity);
    }

    // Helper to convert Sensor entity to a NodeDto representation
    private static NodeDto fromSensorEntity(Sensor sensorEntity) {
        NodeDto dto = new NodeDto();
        dto.setId(sensorEntity.getId());
        dto.setName(sensorEntity.getName());
        dto.setType("sensor");
        dto.setMetadata(sensorEntity.getMetadata());
        dto.setHasChildren(false);
        // A sensor's parent is the node it's attached to
        if (sensorEntity.getNode() != null) {
            dto.setParentId(sensorEntity.getNode().getId());
        }
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
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
}
