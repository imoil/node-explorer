package com.example.treeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Entity
@Getter
@Setter
public class Sensor {

    @Id
    private String id;

    private String name;

    // ✨ 'type' 필드를 추가합니다.
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private Node node;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sensor_metadata", joinColumns = @JoinColumn(name = "sensor_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
}