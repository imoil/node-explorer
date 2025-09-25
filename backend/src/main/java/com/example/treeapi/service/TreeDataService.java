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
                .sorted(Comparator.comparing(NodeDto::getName))
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
                    .map(sensor -> {
                        NodeDto dto = toSensorDto(sensor);
                        dto.setParentId(parent.getId().toString()); // Set correct parent ID
                        return dto;
                    })
                    .collect(Collectors.toList());
            childNodes.addAll(sensorDtos);
        });

        childNodes.sort(Comparator.comparing(NodeDto::getName));

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
        LinkedList<NodeDto> path = new LinkedList<>();
        Node currentNode = node;
        while (currentNode != null && currentNode.getParentId() != null) {
            path.addFirst(toNodeDto(currentNode));
            currentNode = nodeRepository.findById(currentNode.getParentId()).orElse(null);
        }
        if (currentNode != null) {
            path.addFirst(toNodeDto(currentNode));
        }
        // The absolute root (ID=1) is virtual, so remove it if it's in the path
        if (!path.isEmpty() && "1".equals(path.getFirst().getId())) {
            path.removeFirst();
        }
        return path;
    }

    private NodeDto toNodeDto(Node node) {
        NodeDto dto = new NodeDto();
        dto.setId(node.getId().toString());
        dto.setName(node.getNodeName());
        dto.setType("folder");
        if (node.getParentId() != null) {
            dto.setParentId(node.getParentId().toString());
        }
        // Compute hasChildren on the fly
        dto.setHasChildren(nodeRepository.hasChildren(node.getId()) || !node.getSensors().isEmpty());
        dto.setMetadata(Collections.emptyMap());
        return dto;
    }

    private NodeDto toSensorDto(Sensor sensor) {
        NodeDto dto = new NodeDto();
        dto.setId(sensor.getId().toString());
        dto.setName(sensor.getSensorName());
        dto.setType("sensor");
        dto.setHasChildren(false);
        dto.setMetadata(Collections.emptyMap());
        return dto;
    }
}