package com.bemfeito.api.location.state.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bemfeito.api.location.state.dto.StateResponse;
import com.bemfeito.api.location.state.repository.StateRepository;

/**
 * Reúne as regras relacionadas à consulta de estados.
 */
@Service
public class StateService {

    private final StateRepository stateRepository;

    public StateService(StateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    /**
     * Retorna todos os estados em ordem alfabética.
     */
    @Transactional(readOnly = true)
    public List<StateResponse> findAll() {
        return stateRepository
            .findAllByOrderByNameAsc()
            .stream()
            .map(StateResponse::from)
            .toList();
    }
}