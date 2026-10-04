package com.bemfeito.api.location.state.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa um estado brasileiro utilizado na localização das unidades.
 */
@Entity
@Table(name = "states")
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "name",
        nullable = false,
        unique = true,
        length = 100
    )
    private String name;

    @Column(
        name = "uf",
        nullable = false,
        unique = true,
        length = 2
    )
    private String uf;

    protected State() {
        // Construtor exigido pelo JPA.
    }

    public State(String name, String uf) {
        this.name = name;
        this.uf = uf;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUf() {
        return uf;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}