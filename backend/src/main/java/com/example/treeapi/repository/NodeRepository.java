package com.example.treeapi.repository;

import com.example.treeapi.domain.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Node, String> {

    List<Node> findByParentId(String parentId);

    List<Node> findByNameContainingIgnoreCase(String query);
}
