package com.example.treeapi.service;

import com.example.treeapi.domain.Node;
import com.example.treeapi.domain.Sensor;
import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.repository.NodeRepository;
import com.example.treeapi.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreeDataService {

    private final NodeRepository nodeRepository;
    private final SensorRepository sensorRepository;

    public List<NodeDto> getRootNodes() {
        // In the new schema, root nodes are children of a virtual node with ID 1.
        return nodeRepository.findByParentId(1L).stream()
                .map(this::toNodeDto)
                .collect(Collectors.toList());
    }

    public List<NodeDto> getChildrenOf(Long parentId) {
        if (parentId == null) {
            return getRootNodes();
        }

        List<NodeDto> childNodes = nodeRepository.findByParentId(parentId).stream()
                .map(this::toNodeDto)
                .collect(Collectors.toList());

        // Since the relationship is now ManyToMany, we fetch the parent and get sensors.
        nodeRepository.findById(parentId).ifPresent(parent -> {
            List<NodeDto> sensorDtos = parent.getSensors().stream()
                    .map(this::toSensorDto)
                    .collect(Collectors.toList());
            childNodes.addAll(sensorDtos);
        });

        return childNodes;
    }

    public List<SearchResultDto> searchNodes(String query) {
        List<SearchResultDto> nodeResults = nodeRepository.findByNodeNameContainingIgnoreCase(query).stream()
                .map(node -> new SearchResultDto(node.getId().toString(), node.getNodeName(), "folder", findPath(node)))
                .collect(Collectors.toList());

        List<SearchResultDto> sensorResults = sensorRepository.findBySensorNameContainingIgnoreCase(query).stream()
                .flatMap(sensor -> sensor.getNodes().stream()
                        .map(node -> new SearchResultDto(sensor.getId().toString(), sensor.getSensorName(), "sensor", findPath(node))))
                .collect(Collectors.toList());

        return Stream.concat(nodeResults.stream(), sensorResults.stream()).collect(Collectors.toList());
    }

    public RevealPathDto revealPath(Long nodeId) {
        return nodeRepository.findById(nodeId)
                .map(node -> {
                    List<NodeDto> path = findPath(node);
                    Map<String, List<NodeDto>> childrenMap = new HashMap<>();
                    path.stream()
                        .filter(p -> !p.getId().equals(nodeId.toString())) // Exclude the target node itself
                        .forEach(p -> childrenMap.put(p.getId(), getChildrenOf(Long.parseLong(p.getId()))));
                    return new RevealPathDto(path, childrenMap);
                })
                .orElse(new RevealPathDto(Collections.emptyList(), Collections.emptyMap()));
    }

    private List<NodeDto> findPath(Node node) {
        if (node == null || node.getNodePath() == null || node.getNodePath().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> pathParts = Arrays.asList(node.getNodePath().split("\|"));
        Map<String, String> pathToNameMap = pathParts.stream()
            .collect(Collectors.toMap(Function.identity(), part -> part));

        List<Node> nodesInPath = nodeRepository.findByNodePathStartingWith(pathParts.get(0));
        
        Map<String, Node> nodeMap = nodesInPath.stream()
            .collect(Collectors.toMap(Node::getNodePath, Function.identity()));

        List<NodeDto> resultPath = new ArrayList<>();
        StringBuilder currentPath = new StringBuilder();
        for (String part : pathParts) {
            if (currentPath.length() > 0) {
                currentPath.append("|");
            }
            currentPath.append(part);
            Node pathNode = nodeMap.get(currentPath.toString());
            if (pathNode != null) {
                resultPath.add(toNodeDto(pathNode));
            }
        }
        return resultPath;
    }

    private NodeDto toNodeDto(Node node) {
        NodeDto dto = new NodeDto();
        dto.setId(node.getId().toString());
        dto.setName(node.getNodeName());
        dto.setType("folder");
        // Compute hasChildren on the fly
        dto.setHasChildren(nodeRepository.hasChildren(node.getId()) || !node.getSensors().isEmpty());
        return dto;
    }

    private NodeDto toSensorDto(Sensor sensor) {
        NodeDto dto = new NodeDto();
        dto.setId(sensor.getId().toString());
        dto.setName(sensor.getSensorName());
        dto.setType("sensor");
        dto.setHasChildren(false);
        return dto;
    }
}