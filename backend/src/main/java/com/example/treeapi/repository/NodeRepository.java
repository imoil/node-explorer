package com.example.treeapi.repository;

import com.example.treeapi.domain.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NodeRepository extends JpaRepository<Node, String> {
    List<Node> findByParentId(String parentId);
    List<Node> findByNameContainingIgnoreCase(String name);

    /**
     * 메타데이터의 값(value)에 검색어가 포함된 Node를 찾습니다.
     */
    @Query("SELECT n FROM Node n JOIN n.metadata m WHERE LOWER(m) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Node> findByMetadataValueContainingIgnoreCase(@Param("query") String query);
}