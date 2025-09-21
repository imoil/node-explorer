package com.example.treeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "SENSOR_INFO")
@Getter
@Setter
public class Sensor {

    @Id
    private Long id;

    @Column(name = "SENSOR_NAME")
    private String sensorName;

    @ManyToMany(mappedBy = "sensors")
    private Set<Node> nodes = new HashSet<>();
}
