package com.example.treeapi.service;

import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.SearchResultDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TreeDataService {

    private final Map<String, NodeDto> allNodesMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> parentToChildrenMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> nodeToSensorsMap = new ConcurrentHashMap<>();

    public TreeDataService() {
        initializeMockData();
    }

    private void initializeMockData() {
        // Root Nodes
        NodeDto manufacturing = new NodeDto("node-1", "Manufacturing (53)", true, Map.of("owner", "Alice", "last_updated", "2025-09-18"));
        NodeDto logistics = new NodeDto("node-2", "Logistics (28)", true, Map.of("owner", "Bob", "region", "Global"));
        NodeDto rAndD = new NodeDto("node-3", "R&D (15)", false, Map.of("budget", "$5M"));

        allNodesMap.put(manufacturing.getId(), manufacturing);
        allNodesMap.put(logistics.getId(), logistics);
        allNodesMap.put(rAndD.getId(), rAndD);
        parentToChildrenMap.put("root", List.of(manufacturing.getId(), logistics.getId(), rAndD.getId()));

        // Level 2 - Children of Manufacturing
        NodeDto factoryA = new NodeDto("node-1-1", "Factory A (EU)", true, Map.of("location", "Germany"));
        NodeDto factoryB = new NodeDto("node-1-2", "Factory B (APAC)", false, Map.of("location", "South Korea"));

        allNodesMap.put(factoryA.getId(), factoryA);
        allNodesMap.put(factoryB.getId(), factoryB);
        parentToChildrenMap.put(manufacturing.getId(), List.of(factoryA.getId(), factoryB.getId()));

        // Level 3 - Children of Factory A & Sensors for Factory A
        NodeDto productionLine1 = new NodeDto("node-1-1-1", "Production Line 1 (21)", false, Map.of("product", "Widget A"));
        NodeDto productionLine2 = new NodeDto("node-1-1-2", "Production Line 2 (89)", false, Map.of("product", "Widget B"));
        allNodesMap.put(productionLine1.getId(), productionLine1);
        allNodesMap.put(productionLine2.getId(), productionLine2);
        parentToChildrenMap.put(factoryA.getId(), List.of(productionLine1.getId(), productionLine2.getId()));

        NodeDto sensorTemp = new NodeDto("sensor-temp-1", "Temperature Sensor", Map.of("unit", "Celsius", "value", "25.5"));
        NodeDto sensorHumidity = new NodeDto("sensor-humidity-1", "Humidity Sensor", Map.of("unit", "%", "value", "60"));
        allNodesMap.put(sensorTemp.getId(), sensorTemp);
        allNodesMap.put(sensorHumidity.getId(), sensorHumidity);
        nodeToSensorsMap.put(factoryA.getId(), List.of(sensorTemp.getId(), sensorHumidity.getId()));

        // Children of Logistics & Sensor for Logistics
        NodeDto warehouseX = new NodeDto("node-2-1", "Warehouse X (US)", false, Map.of("manager", "Charlie"));
        allNodesMap.put(warehouseX.getId(), warehouseX);
        parentToChildrenMap.put(logistics.getId(), List.of(warehouseX.getId()));
        
        NodeDto sensorGps = new NodeDto("sensor-logistics-1", "GPS Tracker", Map.of("battery", "80%", "accuracy", "5m"));
        allNodesMap.put(sensorGps.getId(), sensorGps);
        nodeToSensorsMap.put(logistics.getId(), List.of(sensorGps.getId()));
    }
    
    public List<NodeDto> getRootNodes() {
        return parentToChildrenMap.getOrDefault("root", List.of()).stream()
                .map(this::getNodeWithSensors)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<NodeDto> getChildrenOf(String parentId) {
        List<NodeDto> children = parentToChildrenMap.getOrDefault(parentId, Collections.emptyList()).stream()
                .map(this::getNodeWithSensors) // 자식 폴더도 센서를 가질 수 있으므로 이 메소드를 사용
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<NodeDto> sensors = nodeToSensorsMap.getOrDefault(parentId, Collections.emptyList()).stream()
                .map(allNodesMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<NodeDto> combinedList = new ArrayList<>();
        combinedList.addAll(children);
        combinedList.addAll(sensors);
        
        return combinedList;
    }

    private NodeDto getNodeWithSensors(String nodeId) {
        NodeDto node = allNodesMap.get(nodeId);
        if (node != null && "folder".equals(node.getType())) {
            List<NodeDto> sensors = nodeToSensorsMap.getOrDefault(nodeId, Collections.emptyList()).stream()
                .map(allNodesMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            node.setSensors(sensors);
        }
        return node;
    }
    
    public List<SearchResultDto> searchNodes(String query) {
        String lowerCaseQuery = query.toLowerCase();

        return allNodesMap.values().stream()
            .filter(node -> {
                boolean nameMatches = node.getName().toLowerCase().contains(lowerCaseQuery);
                boolean metadataMatches = node.getMetadata() != null && node.getMetadata().values().stream()
                        .anyMatch(val -> val.toLowerCase().contains(lowerCaseQuery));
                return nameMatches || metadataMatches;
            })
            .map(node -> new SearchResultDto(node.getId(), node.getName(), node.getType(), findPathToNode(node.getId())))
            .collect(Collectors.toList());
    }
    
    public List<NodeDto> findPathToNode(String nodeId) {
        LinkedList<NodeDto> path = new LinkedList<>();
        String currentId = nodeId;

        while (currentId != null && !"root".equals(currentId)) {
            NodeDto currentNode = allNodesMap.get(currentId);
            if (currentNode != null) {
                path.addFirst(currentNode);
            }

            String finalCurrentId = currentId;
            Optional<String> parentIdOpt = parentToChildrenMap.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(finalCurrentId))
                    .map(Map.Entry::getKey)
                    .findFirst();

            if (parentIdOpt.isPresent()) {
                currentId = parentIdOpt.get();
            } else {
                // If not found as a child, check if it's a sensor
                currentId = nodeToSensorsMap.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(finalCurrentId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            }
        }
        return path;
    }
    
    public Map<String, NodeDto> getAllNodesMap() {
        return allNodesMap;
    }

    public String updateRandomNodeName(String nodeId) {
        NodeDto node = allNodesMap.get(nodeId);
        if (node != null) {
            Random random = new Random();
            String newName = node.getName().replaceAll(" \\(\\d+\\)$", "") + " (" + (random.nextInt(90) + 10) + ")";
            node.setName(newName);
            return newName;
        }
        return null;
    }
}

