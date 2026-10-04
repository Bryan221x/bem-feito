package com.bemfeito.api.location.state.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bemfeito.api.location.state.entity.State;

/**
 * Acesso aos estados armazenados no banco de dados.
 */
public interface StateRepository extends JpaRepository<State, Long> {

    Optional<State> findByUfIgnoreCase(String uf);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByUfIgnoreCase(String uf);
}