package com.example.treeapi.controller;

import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchRequest;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.service.TreeDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NodeController {
    private final TreeDataService treeDataService;

    @GetMapping("/nodes/root")
    public List<NodeDto> getRootNodes() {
        return treeDataService.getRootNodes();
    }

    @GetMapping("/nodes/{id}/children")
    public List<NodeDto> getChildren(@PathVariable @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$", message = "Invalid node ID format") String id) {
        return treeDataService.getChildrenOf(id);
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> searchNodes(
            @RequestBody @Valid SearchRequest request) {
        List<SearchResultDto> results = treeDataService.searchNodes(request.getQuery());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/reveal-path/{nodeId}")
    public RevealPathDto revealPath(@PathVariable @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$") String nodeId) {
        return treeDataService.revealPath(nodeId);
    }
}