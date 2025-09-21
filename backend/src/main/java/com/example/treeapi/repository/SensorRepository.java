package com.example.treeapi.repository;

import com.example.treeapi.domain.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    List<Sensor> findBySensorNameContainingIgnoreCase(String sensorName);

}
