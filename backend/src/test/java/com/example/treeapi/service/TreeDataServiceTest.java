package com.example.treeapi.service;

import com.example.treeapi.domain.Node;
import com.example.treeapi.domain.Sensor;
import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.repository.NodeRepository;
import com.example.treeapi.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TreeDataServiceTest {

    @Autowired
    private TreeDataService treeDataService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private SensorRepository sensorRepository;

    private Node root;
    private Node child1;
    private Node child2;
    private Node grandchild1;
    private Sensor sensor1;

    @BeforeEach
    void setUp() {
        // Clear existing data before each test to ensure isolation
        nodeRepository.deleteAll();
        sensorRepository.deleteAll();

        // Create a consistent test data set
        root = createNode(1L, "ROOT", null, "ROOT");
        child1 = createNode(101L, "ROOT1", 1L, "ROOT1");
        child2 = createNode(102L, "ROOT2", 1L, "ROOT2");
        grandchild1 = createNode(104L, "NODE1", 101L, "ROOT1|NODE1");
        sensor1 = createSensor(201L, "SENSOR1", child1);
    }

    private Node createNode(Long id, String name, Long parentId, String nodePath) {
        Node node = new Node();
        node.setId(id);
        node.setNodeName(name);
        node.setParentId(parentId);
        node.setNodePath(nodePath);
        return nodeRepository.save(node);
    }

    private Sensor createSensor(Long id, String name, Node parentNode) {
        Sensor sensor = new Sensor();
        sensor.setId(id);
        sensor.setSensorName(name);
        sensor.setNodes(Set.of(parentNode));
        sensorRepository.save(sensor);
        parentNode.getSensors().add(sensor);
        nodeRepository.save(parentNode);
        return sensor;
    }

    @Test
    void testGetRootNodes() {
        List<NodeDto> rootNodes = treeDataService.getRootNodes();
        assertThat(rootNodes).hasSize(2);
        assertThat(rootNodes).extracting(NodeDto::getName).contains("ROOT1", "ROOT2");
    }

    @Test
    void testGetChildrenOf() {
        // Test node with a folder child and a sensor child
        List<NodeDto> childrenOfChild1 = treeDataService.getChildrenOf(101L);
        assertThat(childrenOfChild1).hasSize(2);
        assertThat(childrenOfChild1).extracting(NodeDto::getName).contains("NODE1", "SENSOR1");
        assertThat(childrenOfChild1).extracting(NodeDto::getType).contains("folder", "sensor");

        // Test node with no children
        List<NodeDto> childrenOfChild2 = treeDataService.getChildrenOf(102L);
        assertThat(childrenOfChild2).isEmpty();
    }

    @Test
    void testSearchNodes() {
        List<SearchResultDto> results = treeDataService.searchNodes("ROOT");
        assertThat(results).hasSize(3);
        assertThat(results).extracting(SearchResultDto::getName).contains("ROOT1", "ROOT2");

        List<SearchResultDto> sensorResults = treeDataService.searchNodes("SENSOR");
        assertThat(sensorResults).hasSize(1);
        assertThat(sensorResults.get(0).getName()).isEqualTo("SENSOR1");
    }

    @Test
    void testRevealPath() {
        RevealPathDto revealData = treeDataService.revealPath(104L);

        // Check path
        assertThat(revealData.getPath()).hasSize(2);
        assertThat(revealData.getPath()).extracting(NodeDto::getId).containsExactly("101", "104");

        // Check children map
        assertThat(revealData.getChildrenMap()).hasSize(1); // Only parent of target node
        assertThat(revealData.getChildrenMap().get("101")).hasSize(2); // NODE1 and SENSOR1
    }
}
