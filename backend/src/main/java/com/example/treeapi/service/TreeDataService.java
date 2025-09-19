package com.example.treeapi.service;

import com.example.treeapi.domain.Node;
import com.example.treeapi.domain.Sensor;
import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.repository.NodeRepository;
import com.example.treeapi.repository.SensorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TreeDataService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @PostConstruct
    @Transactional
    public void initializeMockData() {
        // Clear existing data
        nodeRepository.deleteAll();
        sensorRepository.deleteAll();

        // Root Nodes
        Node manufacturing = createNode("node-1", "Manufacturing (53)", null, true, Map.of("owner", "Alice", "last_updated", "2025-09-18"));
        Node logistics = createNode("node-2", "Logistics (28)", null, true, Map.of("owner", "Bob", "region", "Global"));
        Node rAndD = createNode("node-3", "R&D (15)", null, false, Map.of("budget", "$5M"));

        // Level 2
        Node factoryA = createNode("node-1-1", "Factory A (EU)", "node-1", true, Map.of("location", "Germany"));
        Node factoryB = createNode("node-1-2", "Factory B (APAC)", "node-1", false, Map.of("location", "South Korea"));
        Node warehouseX = createNode("node-2-1", "Warehouse X (US)", "node-2", false, Map.of("manager", "Charlie"));

        // Level 3
        Node productionLine1 = createNode("node-1-1-1", "Production Line 1 (21)", "node-1-1", false, Map.of("product", "Widget A"));
        Node productionLine2 = createNode("node-1-1-2", "Production Line 2 (89)", "node-1-1", false, Map.of("product", "Widget B"));

        // Sensors
        createSensor("sensor-temp-1", "Temperature Sensor", factoryA, Map.of("unit", "Celsius", "value", "25.5"));
        createSensor("sensor-humidity-1", "Humidity Sensor", factoryA, Map.of("unit", "%", "value", "60"));
        createSensor("sensor-logistics-1", "GPS Tracker", logistics, Map.of("battery", "80%", "accuracy", "5m"));
    }

    private Node createNode(String id, String name, String parentId, boolean hasChildren, Map<String, String> metadata) {
        Node node = new Node();
        node.setId(id);
        node.setName(name);
        node.setParentId(parentId);
        node.setHasChildren(hasChildren);
        node.setType("folder");
        node.setMetadata(metadata);
        return nodeRepository.save(node);
    }

    private void createSensor(String id, String name, Node parentNode, Map<String, String> metadata) {
        Sensor sensor = new Sensor();
        sensor.setId(id);
        sensor.setName(name);
        sensor.setNode(parentNode);
        sensor.setMetadata(metadata);
        sensorRepository.save(sensor);
    }

    @Transactional(readOnly = true)
    public List<NodeDto> getRootNodes() {
        return nodeRepository.findByParentId(null).stream()
                .map(NodeDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NodeDto> getChildrenOf(String parentId) {
        // Get child folders
        List<NodeDto> children = nodeRepository.findByParentId(parentId).stream()
                .map(NodeDto::new)
                .collect(Collectors.toList());

        // Get attached sensors
        nodeRepository.findById(parentId).ifPresent(parentNode -> {
            List<NodeDto> sensors = parentNode.getSensors().stream()
                    .map(sensor -> {
                        NodeDto dto = new NodeDto();
                        dto.setId(sensor.getId());
                        dto.setName(sensor.getName());
                        dto.setType("sensor");
                        dto.setMetadata(sensor.getMetadata());
                        return dto;
                    })
                    .collect(Collectors.toList());
            children.addAll(sensors);
        });

        return children;
    }

    @Transactional(readOnly = true)
    public List<SearchResultDto> searchNodes(String query) {
        return nodeRepository.findByNameContainingIgnoreCase(query).stream()
                .map(node -> new SearchResultDto(node.getId(), node.getName(), node.getType(), findPathToNode(node.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NodeDto> findPathToNode(String nodeId) {
        LinkedList<NodeDto> path = new LinkedList<>();
        Optional<Node> currentNodeOpt = nodeRepository.findById(nodeId);

        while (currentNodeOpt.isPresent()) {
            Node currentNode = currentNodeOpt.get();
            path.addFirst(new NodeDto(currentNode));
            if (currentNode.getParentId() != null) {
                currentNodeOpt = nodeRepository.findById(currentNode.getParentId());
            } else {
                currentNodeOpt = Optional.empty();
            }
        }
        return path;
    }

    @Transactional(readOnly = true)
    public RevealPathDto revealPath(String nodeId) {
        List<NodeDto> path = findPathToNode(nodeId);
        Map<String, List<NodeDto>> childrenMap = new HashMap<>();

        // For each node in the path, get its children so the frontend can render the expanded tree
        for (NodeDto nodeInPath : path) {
            // We don't need the children of the final target node itself, just the path to it.
            if (!nodeInPath.getId().equals(nodeId)) {
                childrenMap.put(nodeInPath.getId(), getChildrenOf(nodeInPath.getId()));
            }
        }
        return new RevealPathDto(path, childrenMap);
    }

    @Transactional
    public String updateRandomNodeName(String nodeId) {
        return nodeRepository.findById(nodeId).map(node -> {
            Random random = new Random();
            String newName = node.getName().replaceAll(" \\(\\d+\\)", "") + " (" + (random.nextInt(90) + 10) + ")";
            node.setName(newName);
            nodeRepository.save(node);
            return newName;
        }).orElse(null);
    }
}