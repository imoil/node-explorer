package com.example.treeapi.repository;

import com.example.treeapi.domain.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NodeRepository extends JpaRepository<Node, Long> {

    List<Node> findByParentId(Long parentId);

    List<Node> findByNodeNameContainingIgnoreCase(String nodeName);

    @Query("SELECT n FROM Node n WHERE n.nodePath LIKE :pathPrefix%")
    List<Node> findByNodePathStartingWith(@Param("pathPrefix") String pathPrefix);

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Node n WHERE n.parentId = :nodeId")
    boolean hasChildren(@Param("nodeId") Long nodeId);
}
