package com.example.treeapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "NODE_INFO")
@Getter
@Setter
public class Node {

    @Id
    private Long id;

    @Column(name = "NODE_PATH")
    private String nodePath;

    @Column(name = "NODE_NAME")
    private String nodeName;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "NODE_SENSOR_MAP",
        joinColumns = @JoinColumn(name = "NODE_ID"),
        inverseJoinColumns = @JoinColumn(name = "SENSOR_ID")
    )
    private Set<Sensor> sensors = new HashSet<>();

    @Transient
    private boolean hasChildren;
}