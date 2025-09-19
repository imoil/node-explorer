package com.example.treeapi.service;

import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TreeDataServiceTest {

    @Autowired
    private TreeDataService treeDataService;

    @Autowired
    private NodeRepository nodeRepository;

    @Test
    void contextLoads() {
        assertThat(treeDataService).isNotNull();
        assertThat(nodeRepository).isNotNull();
    }

    @Test
    void testDataInitialization() {
        // Data is initialized via @PostConstruct, just verify the count
        long nodeCount = nodeRepository.count();
        assertThat(nodeCount).isEqualTo(8); // 8 folder nodes in mock data
    }

    @Test
    void testGetRootNodes() {
        List<NodeDto> rootNodes = treeDataService.getRootNodes();
        assertThat(rootNodes).hasSize(3);
        assertThat(rootNodes).extracting(NodeDto::getName).contains("Manufacturing (53)", "Logistics (28)", "R&D (15)");
    }

    @Test
    void testGetChildrenOf() {
        // Test node with folder children and sensor children
        List<NodeDto> childrenOfFactoryA = treeDataService.getChildrenOf("node-1-1");
        assertThat(childrenOfFactoryA).hasSize(4); // 2 folders + 2 sensors
        assertThat(childrenOfFactoryA).extracting(NodeDto::getName).contains("Production Line 1 (21)", "Production Line 2 (89)", "Temperature Sensor", "Humidity Sensor");

        // Test node with no children
        List<NodeDto> childrenOfRandD = treeDataService.getChildrenOf("node-3");
        assertThat(childrenOfRandD).isEmpty();
    }

    @Test
    void testFindPathToNode() {
        // Test a deeply nested node
        List<NodeDto> path = treeDataService.findPathToNode("node-1-1-1");
        assertThat(path).hasSize(3);
        assertThat(path).extracting(NodeDto::getId).containsExactly("node-1", "node-1-1", "node-1-1-1");
    }

    @Test
    void testSearchNodes() {
        List<SearchResultDto> results = treeDataService.searchNodes("factory");
        assertThat(results).hasSize(2);
        assertThat(results).extracting(SearchResultDto::getName).contains("Factory A (EU)", "Factory B (APAC)");
    }

    @Test
    void testRevealPath() {
        RevealPathDto revealData = treeDataService.revealPath("node-1-1-1");

        // Check path
        assertThat(revealData.getPath()).hasSize(3);
        assertThat(revealData.getPath()).extracting(NodeDto::getId).containsExactly("node-1", "node-1-1", "node-1-1-1");

        // Check children map
        assertThat(revealData.getChildrenMap()).hasSize(2);
        assertThat(revealData.getChildrenMap().get("node-1")).hasSize(2); // Factory A, Factory B
        assertThat(revealData.getChildrenMap().get("node-1-1")).hasSize(4); // 2 prod lines + 2 sensors
    }
}
