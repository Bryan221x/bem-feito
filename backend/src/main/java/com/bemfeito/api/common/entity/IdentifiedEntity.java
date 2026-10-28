package com.bemfeito.api.common.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class IdentifiedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() { return id; }
}
