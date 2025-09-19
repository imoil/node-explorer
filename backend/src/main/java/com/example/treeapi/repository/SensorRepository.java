package com.example.treeapi.repository;

import com.example.treeapi.domain.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findByNameContainingIgnoreCase(String name);

    /**
     * 메타데이터의 값(value)에 검색어가 포함된 Sensor를 찾습니다.
     */
    @Query("SELECT s FROM Sensor s JOIN s.metadata m WHERE LOWER(m) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Sensor> findByMetadataValueContainingIgnoreCase(@Param("query") String query);
}