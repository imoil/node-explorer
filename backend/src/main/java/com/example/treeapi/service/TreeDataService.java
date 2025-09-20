package com.example.treeapi.service;

import com.example.treeapi.domain.Node;
import com.example.treeapi.domain.Sensor;
import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.repository.NodeRepository;
import com.example.treeapi.repository.SensorRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TreeDataService {
    private final NodeRepository nodeRepository;
    private final SensorRepository sensorRepository;

    @PostConstruct
    @Transactional
    public void initializeMockData() {
        // Clear existing data
        sensorRepository.deleteAll();
        nodeRepository.deleteAll();

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
        sensor.setType("sensor");
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
        // Handle root case explicitly to avoid findById(null)
        if (parentId == null) {
            return getRootNodes();
        }

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
                        dto.setHasChildren(false);
                        dto.setMetadata(sensor.getMetadata());
                        return dto;
                    })
                    .collect(Collectors.toList());
            children.addAll(sensors);
        });

        return children;
    }

    /**
     * Node와 Sensor 모두에서 이름과 메타데이터 속성으로 검색을 수행합니다.
     */
    @Transactional(readOnly = true)
    public List<SearchResultDto> searchNodes(String query) {
        // 이름으로 검색 + 속성으로 검색 후 중복 제거
        List<Node> matchingNodes = Stream.concat(
            nodeRepository.findByNameContainingIgnoreCase(query).stream(),
            nodeRepository.findByMetadataValueContainingIgnoreCase(query).stream()
        ).distinct().collect(Collectors.toList());

        List<Sensor> matchingSensors = Stream.concat(
            sensorRepository.findByNameContainingIgnoreCase(query).stream(),
            sensorRepository.findByMetadataValueContainingIgnoreCase(query).stream()
        ).distinct().collect(Collectors.toList());

        // 검색 결과를 DTO로 변환
        Stream<SearchResultDto> nodeResults = matchingNodes.stream()
            .map(node -> new SearchResultDto(node.getId(), node.getName(), node.getType(), findPathToNode(node.getId())));

        Stream<SearchResultDto> sensorResults = matchingSensors.stream()
            .map(sensor -> new SearchResultDto(sensor.getId(), sensor.getName(), sensor.getType(), findPathToNode(sensor.getId())));

        // Node와 Sensor 검색 결과를 합쳐서 반환
        return Stream.concat(nodeResults, sensorResults).collect(Collectors.toList());
    }

    /**
     * ID를 기반으로 Node 또는 Sensor의 경로를 찾습니다.
     * Sensor ID가 주어지면 부모 Node의 경로를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<NodeDto> findPathToNode(String itemId) {
        LinkedList<NodeDto> path = new LinkedList<>();
        Optional<Node> startNodeOpt;

        // itemId가 Node인지 확인
        Optional<Node> nodeAsNode = nodeRepository.findById(itemId);
        if (nodeAsNode.isPresent()) {
            startNodeOpt = nodeAsNode;
        } else {
            // Node가 아니라면 Sensor인지 확인하고, 맞다면 부모 Node를 경로의 시작점으로 설정
            Optional<Sensor> sensorAsSensor = sensorRepository.findById(itemId);
            startNodeOpt = sensorAsSensor.map(Sensor::getNode);
        }

        // 경로의 시작점이 정해졌으면, 최상위 부모까지 경로를 재구성
        Optional<Node> currentNodeOpt = startNodeOpt;
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

        if (path.isEmpty()) {
            return new RevealPathDto(Collections.emptyList(), Collections.emptyMap());
        }

        // Per the test case, only add children for nodes in the path that are not the final target.
        for (NodeDto nodeInPath : path) {
            if (!nodeInPath.getId().equals(nodeId)) {
                childrenMap.put(nodeInPath.getId(), getChildrenOf(nodeInPath.getId()));
            }
        }

        return new RevealPathDto(path, childrenMap);
    }

}