package com.bemfeito.api.location.city.entity;

import com.bemfeito.api.location.state.entity.State;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(
    name = "uq_cities_state_name", columnNames = {"state_id", "name"}))
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @Column(nullable = false, length = 150)
    private String name;

    protected City() {}

    public City(State state, String name) {
        this.state = state;
        this.name = name;
    }

    public Long getId() { return id; }
    public State getState() { return state; }
    public String getName() { return name; }
}
