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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // Make sure we use the test profile
class TreeDataServiceTest {

    @Autowired
    private TreeDataService treeDataService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @BeforeEach
    void setUp() {
        // Clear existing data before each test to ensure isolation
        sensorRepository.deleteAllInBatch();
        nodeRepository.deleteAllInBatch();

        // Create a consistent test data set
        Node root1 = createNode("root|1", "Manufacturing", null, true, Map.of("owner", "Alice"));
        Node root2 = createNode("root|2", "Logistics", null, false, Map.of("owner", "Bob"));

        Node factoryA = createNode("root|1|factory-a", "Factory A", "root|1", true, Map.of("location", "Germany"));
        Node factoryB = createNode("root|1|factory-b", "Factory B", "root|1", false, Map.of("location", "Korea"));

        Node line1 = createNode("root|1|factory-a|line-1", "Production Line 1", "root|1|factory-a", false, Map.of());

        createSensor("sensor|temp|1", "Temperature Sensor", factoryA, Map.of("unit", "C"));
        createSensor("sensor|humidity|1", "Humidity Sensor", factoryA, Map.of("unit", "%"));
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

    @Test
    void testGetRootNodes() {
        List<NodeDto> rootNodes = treeDataService.getRootNodes();
        assertThat(rootNodes).hasSize(2);
        assertThat(rootNodes).extracting(NodeDto::getName).contains("Manufacturing", "Logistics");
    }

    @Test
    void testGetChildrenOf() {
        // Test node with folder children and sensor children
        List<NodeDto> childrenOfFactoryA = treeDataService.getChildrenOf("root|1|factory-a");
        assertThat(childrenOfFactoryA).hasSize(3); // 1 folder + 2 sensors
        assertThat(childrenOfFactoryA).extracting(NodeDto::getName).contains("Production Line 1", "Temperature Sensor", "Humidity Sensor");

        // Test node with no children
        List<NodeDto> childrenOfLogistics = treeDataService.getChildrenOf("root|2");
        assertThat(childrenOfLogistics).isEmpty();
    }

    @Test
    void testFindPathToNode() {
        // Test a deeply nested node
        List<NodeDto> path = treeDataService.findPathToNode("root|1|factory-a|line-1");
        assertThat(path).hasSize(3);
        assertThat(path).extracting(NodeDto::getId).containsExactly("root|1", "root|1|factory-a", "root|1|factory-a|line-1");
    }

    @Test
    void testSearchNodes() {
        List<SearchResultDto> results = treeDataService.searchNodes("Factory");
        assertThat(results).hasSize(2);
        assertThat(results).extracting(SearchResultDto::getName).contains("Factory A", "Factory B");
    }

    @Test
    void testRevealPath() {
        RevealPathDto revealData = treeDataService.revealPath("root|1|factory-a|line-1");

        // Check path
        assertThat(revealData.getPath()).hasSize(3);
        assertThat(revealData.getPath()).extracting(NodeDto::getId).containsExactly("root|1", "root|1|factory-a", "root|1|factory-a|line-1");

        // Check children map
        assertThat(revealData.getChildrenMap()).hasSize(2);
        assertThat(revealData.getChildrenMap().get("root|1")).hasSize(2); // Factory A, Factory B
        assertThat(revealData.getChildrenMap().get("root|1|factory-a")).hasSize(3); // 1 prod line + 2 sensors
    }
}