package com.example.treeapi.controller;

import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchRequest;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.service.TreeDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NodeController {

    @Autowired
    private TreeDataService treeDataService;

    @GetMapping("/nodes/root")
    public List<NodeDto> getRootNodes() {
        return treeDataService.getRootNodes();
    }

    @GetMapping("/nodes/{id}/children")
    public List<NodeDto> getChildren(@PathVariable String id) {
        return treeDataService.getChildrenOf(id);
    }

    @PostMapping("/search")
    public List<SearchResultDto> searchNodes(@RequestBody SearchRequest request) {
        return treeDataService.searchNodes(request.getQuery());
    }

    @GetMapping("/reveal-path/{nodeId}")
    public RevealPathDto revealPath(@PathVariable String nodeId) {
        return treeDataService.revealPath(nodeId);
    }
}

